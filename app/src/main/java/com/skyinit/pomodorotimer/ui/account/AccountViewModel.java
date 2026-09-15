package com.skyinit.pomodorotimer.ui.account;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.AvatarStorage;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.model.ProfileAvatarImage;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.AccountOperationGuard;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 个人资料页 ViewModel（MVI）：资料编辑、头像与会话快捷操作（退出 / 切号）。
 * 修改密码、注销账户已下沉至 {@link AccountSecurityViewModel}。
 */
public class AccountViewModel extends AndroidViewModel
        implements AccountSessionActionCoordinator.Listener {

    private static final String TAG = "AccountViewModel";

    private final UserSessionRepository sessionRepository;
    private final AccountSessionActionCoordinator sessionActions;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<AccountUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<ProfileAvatarImage> avatarImage =
            new MutableLiveData<>(ProfileAvatarImage.none());
    private final SingleLiveEvent<AccountEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger avatarLoadGeneration = new AtomicInteger(0);
    private final AtomicInteger avatarSaveGeneration = new AtomicInteger(0);
    @Nullable
    private Bitmap lastDeliveredAvatar;
    private boolean actionInProgress;
    @Nullable
    private String boundUserId;

    private final Observer<User> activeUserObserver = this::onActiveUserChanged;

    public AccountViewModel(@NonNull Application application,
                            UserSessionRepository sessionRepository,
                            AccountOperationGuard accountOperationGuard) {
        super(application);
        this.sessionRepository = sessionRepository;
        this.sessionActions = new AccountSessionActionCoordinator(
                sessionRepository, accountOperationGuard, this);
        sessionRepository.getActiveUser().observeForever(activeUserObserver);
        User current = sessionRepository.getCurrentUser();
        onActiveUserChanged(current);
    }

    public LiveData<AccountUiState> getUiState() {
        return uiState;
    }

    public LiveData<ProfileAvatarImage> getAvatarImage() {
        return avatarImage;
    }

    public LiveData<AccountEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull AccountIntent intent) {
        switch (intent.type) {
            case OPEN_AVATAR:
                effects.setValue(AccountEffect.showAvatarActions());
                break;
            case VIEW_AVATAR:
                viewAvatar();
                break;
            case CHANGE_AVATAR:
                effects.setValue(AccountEffect.showPickAvatar());
                break;
            case PICK_FROM_GALLERY:
                effects.setValue(AccountEffect.openGallery());
                break;
            case TAKE_PHOTO:
                effects.setValue(AccountEffect.openCamera());
                break;
            case AVATAR_URI_SELECTED:
                if (intent.uri != null) {
                    processAvatarUri(intent.uri);
                }
                break;
            case OPEN_NICKNAME:
                effects.setValue(AccountEffect.startActivity(EditNicknameActivity.class));
                break;
            case OPEN_SIGNATURE:
                effects.setValue(AccountEffect.startActivity(EditSignatureActivity.class));
                break;
            case LOGOUT:
                if (actionInProgress) {
                    return;
                }
                sessionActions.requestLogout();
                break;
            case CONFIRM_LOGOUT:
                if (actionInProgress) {
                    return;
                }
                sessionActions.confirmLogout();
                break;
            case UPGRADE_REGISTER:
                effects.setValue(AccountEffect.startActivity(RegisterActivity.class));
                break;
            case LOGIN:
                effects.setValue(AccountEffect.startActivity(LoginActivity.class));
                break;
            case SWITCH_ACCOUNT:
                if (actionInProgress) {
                    return;
                }
                sessionActions.requestSwitchAccount();
                break;
            case CONTINUE_AFTER_DISABLE_BLOCKING:
                sessionActions.continueAfterDisablingBlocking();
                break;
            default:
                break;
        }
    }

    private void viewAvatar() {
        AccountUiState state = uiState.getValue();
        if (state == null || !state.hasAvatar || state.avatarPath == null) {
            effects.setValue(AccountEffect.showToast(R.string.account_toast_no_avatar));
            return;
        }
        effects.setValue(AccountEffect.openImagePreview(state.avatarPath));
    }

    private void processAvatarUri(@NonNull Uri uri) {
        if (actionInProgress) {
            return;
        }
        AccountUiState state = uiState.getValue();
        if (state == null) {
            return;
        }
        final String userId = state.userId;
        final int generation = avatarSaveGeneration.incrementAndGet();
        setActionInProgress(true);
        Application app = getApplication();
        executors.diskIo(() -> {
            String savedPath = null;
            String error = null;
            try {
                InputStream inputStream = app.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    error = app.getString(R.string.account_toast_image_failed);
                } else {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    if (bitmap == null) {
                        error = app.getString(R.string.account_toast_image_failed);
                    } else {
                        savedPath = AvatarStorage.getInstance().saveJpeg(app, userId, bitmap);
                        if (!bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                        if (savedPath == null) {
                            error = app.getString(R.string.account_toast_image_failed);
                        }
                    }
                }
            } catch (Exception e) {
                AppLog.w(TAG, "Avatar process failed", e);
                error = app.getString(R.string.account_toast_image_failed);
            } finally {
                AvatarStorage.getInstance().deleteCameraCaptureFile(app);
            }
            final String path = savedPath;
            final String err = error;
            mainHandler.post(() -> {
                if (generation != avatarSaveGeneration.get()) {
                    return;
                }
                if (path == null) {
                    setActionInProgress(false);
                    effects.setValue(AccountEffect.showToastText(
                            err != null ? err : app.getString(R.string.account_toast_image_failed)));
                    return;
                }
                sessionRepository.updateProfile(null, path, null,
                        new AccountManager.ProfileUpdateCallback() {
                            @Override
                            public void onSuccess() {
                                if (generation != avatarSaveGeneration.get()) {
                                    return;
                                }
                                setActionInProgress(false);
                                effects.setValue(AccountEffect.showToast(
                                        R.string.account_toast_update_success));
                            }

                            @Override
                            public void onError(String message) {
                                if (generation != avatarSaveGeneration.get()) {
                                    return;
                                }
                                setActionInProgress(false);
                                effects.setValue(AccountEffect.showToastText(message));
                            }
                        });
            });
        });
    }

    private void onActiveUserChanged(@Nullable User user) {
        if (user == null) {
            boundUserId = null;
            sessionActions.onSessionUserChanged();
            if (!sessionActions.isBusy()) {
                actionInProgress = false;
            }
            uiState.setValue(buildGuestUiState());
            avatarImage.setValue(ProfileAvatarImage.none());
            return;
        }
        boolean userSwitched = boundUserId != null && !boundUserId.equals(user.userId);
        if (userSwitched) {
            sessionActions.onSessionUserChanged();
            avatarSaveGeneration.incrementAndGet();
            if (!sessionActions.isBusy()) {
                actionInProgress = false;
            }
        }
        publishFromUser(user);
        loadAvatarAsync(user.avatarPath);
    }

    private void publishFromUser(@NonNull User user) {
        boundUserId = user.userId;
        uiState.setValue(buildUiState(user));
    }

    private AccountUiState buildGuestUiState() {
        Application app = getApplication();
        return new AccountUiState(
                "",
                app.getString(R.string.profile_guest_label),
                app.getString(R.string.profile_login_prompt),
                app.getString(R.string.profile_signature_placeholder),
                false,
                null,
                false,
                false,
                actionInProgress
        );
    }

    private AccountUiState buildUiState(@NonNull User user) {
        Application app = getApplication();
        String idDisplay = user.userId;
        String rawSignature = user.signature != null ? user.signature : "";
        boolean hasCustom = !rawSignature.isEmpty();
        String signatureDisplay = hasCustom
                ? rawSignature
                : app.getString(R.string.profile_signature_placeholder);
        boolean hasAvatar = user.avatarPath != null && !user.avatarPath.isEmpty();
        return new AccountUiState(
                user.userId,
                idDisplay,
                user.nickname != null ? user.nickname : "",
                signatureDisplay,
                hasCustom,
                user.avatarPath,
                hasAvatar,
                true,
                actionInProgress
        );
    }

    private void setActionInProgress(boolean inProgress) {
        actionInProgress = inProgress;
        AccountUiState current = uiState.getValue();
        if (current != null) {
            uiState.setValue(current.withActionInProgress(inProgress));
        }
    }

    private void loadAvatarAsync(@Nullable String avatarPath) {
        final int generation = avatarLoadGeneration.incrementAndGet();
        if (avatarPath == null || avatarPath.isEmpty()) {
            clearAvatarBitmapDelivery();
            avatarImage.setValue(ProfileAvatarImage.none());
            return;
        }
        final String path = avatarPath;
        AppExecutors.getInstance().diskIo(() -> {
            Bitmap circular = null;
            try {
                File file = new File(path);
                if (file.exists()) {
                    Bitmap decoded = BitmapFactory.decodeFile(path);
                    if (decoded != null) {
                        circular = createCircularBitmap(decoded);
                        if (circular != decoded && !decoded.isRecycled()) {
                            decoded.recycle();
                        }
                    }
                }
            } catch (Exception e) {
                AppLog.w(TAG, "Failed to decode avatar", e);
            }
            final Bitmap result = circular;
            mainHandler.post(() -> {
                if (generation != avatarLoadGeneration.get()) {
                    if (result != null && !result.isRecycled()) {
                        result.recycle();
                    }
                    return;
                }
                Bitmap previous = lastDeliveredAvatar;
                lastDeliveredAvatar = result;
                avatarImage.setValue(new ProfileAvatarImage(path, result));
                if (previous != null && previous != result && !previous.isRecycled()) {
                    mainHandler.post(() -> {
                        if (!previous.isRecycled()) {
                            previous.recycle();
                        }
                    });
                }
            });
        });
    }

    private void clearAvatarBitmapDelivery() {
        Bitmap previous = lastDeliveredAvatar;
        lastDeliveredAvatar = null;
        if (previous != null && !previous.isRecycled()) {
            mainHandler.post(() -> {
                if (!previous.isRecycled()) {
                    previous.recycle();
                }
            });
        }
    }

    @Nullable
    private static Bitmap createCircularBitmap(@NonNull Bitmap source) {
        try {
            int size = Math.min(source.getWidth(), source.getHeight());
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float radius = size / 2f;
            Path path = new Path();
            path.addCircle(radius, radius, radius, Path.Direction.CCW);
            canvas.save();
            canvas.clipPath(path);
            int left = (size - source.getWidth()) / 2;
            int top = (size - source.getHeight()) / 2;
            canvas.drawBitmap(source, left, top, paint);
            canvas.restore();
            return output;
        } catch (Exception e) {
            return source;
        }
    }

    @Override
    public void onBusyChanged(boolean busy) {
        setActionInProgress(busy);
    }

    @Override
    public void onToast(int resId) {
        effects.setValue(AccountEffect.showToast(resId));
    }

    @Override
    public void onToastText(@NonNull String message) {
        if (!message.isEmpty()) {
            effects.setValue(AccountEffect.showToastText(message));
        }
    }

    @Override
    public void onGuardPrompt() {
        effects.setValue(AccountEffect.showGuardPrompt());
    }

    @Override
    public void onLogoutConfirm() {
        effects.setValue(AccountEffect.showLogoutConfirm());
    }

    @Override
    public void onDeleteConfirm() {
        // 个人资料页已移除注销入口，协调器不会触发此回调
    }

    @Override
    public void onDeleteSuccess() {
        // 同上
    }

    @Override
    public void onNavigateToLogin() {
        effects.setValue(AccountEffect.startActivity(LoginActivity.class));
    }

    @Override
    public void onLogoutSuccess() {
        effects.setValue(AccountEffect.showToast(R.string.account_logout_success));
    }

    @Override
    protected void onCleared() {
        avatarLoadGeneration.incrementAndGet();
        avatarSaveGeneration.incrementAndGet();
        sessionActions.clear();
        sessionRepository.getActiveUser().removeObserver(activeUserObserver);
        Bitmap previous = lastDeliveredAvatar;
        lastDeliveredAvatar = null;
        if (previous != null && !previous.isRecycled()) {
            previous.recycle();
        }
        super.onCleared();
    }
}
