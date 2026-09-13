package com.skyinit.pomodorotimer.ui.profile;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.model.ProfileAvatarImage;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.UserAppBlockingRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.ui.account.AccountActivity;
import com.skyinit.pomodorotimer.ui.settings.SettingsActivity;
import com.skyinit.pomodorotimer.util.AppBlockingServiceUtils;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;
import com.skyinit.pomodorotimer.util.PermissionUtils;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 「我的」页 ViewModel：Intent → 单一 {@link ProfileUiState} + Effect；generation 丢弃过期异步结果。
 */
public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";

    private final Application application;
    private final UserSessionRepository sessionRepository;
    private final UserAppBlockingRepository userAppBlockingRepository;
    private final StatisticsRepository statisticsRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<ProfileUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<ProfileAvatarImage> avatarImage =
            new MutableLiveData<>(ProfileAvatarImage.none());
    private final SingleLiveEvent<ProfileEffect> effects = new SingleLiveEvent<>();

    private final AtomicInteger statsGeneration = new AtomicInteger(0);
    private final AtomicInteger avatarGeneration = new AtomicInteger(0);

    private int cachedTotalCompletedCount;
    private long cachedTotalFocusDurationMs;
    private boolean statsReady;
    @Nullable
    private String statsUserId;
    private boolean blockingBusy;
    @Nullable
    private Bitmap lastDeliveredAvatar;

    private final Observer<User> activeUserObserver = this::onActiveUserChanged;
    private final Observer<Integer> sessionVersionObserver = unused -> loadStatistics();

    public ProfileViewModel(Application application,
                            UserSessionRepository sessionRepository,
                            UserAppBlockingRepository userAppBlockingRepository,
                            StatisticsRepository statisticsRepository) {
        this.application = application;
        this.sessionRepository = sessionRepository;
        this.userAppBlockingRepository = userAppBlockingRepository;
        this.statisticsRepository = statisticsRepository;

        sessionRepository.getActiveUser().observeForever(activeUserObserver);
        sessionRepository.getSessionVersion().observeForever(sessionVersionObserver);

        User current = sessionRepository.getCurrentUser();
        if (current != null) {
            onActiveUserChanged(current);
        } else {
            publishState(null);
        }
        loadStatistics();
    }

    @NonNull
    public LiveData<ProfileUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<ProfileAvatarImage> getAvatarImage() {
        return avatarImage;
    }

    @NonNull
    public LiveData<ProfileEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull ProfileIntent intent) {
        switch (intent.type) {
            case REFRESH:
                refresh();
                break;
            case TOGGLE_BLOCKING:
                onToggleBlocking(intent.blockingOn);
                break;
            case OPEN_ACCOUNT:
                effects.setValue(ProfileEffect.startActivity(AccountActivity.class));
                break;
            case OPEN_AVATAR_PREVIEW:
                openAvatarPreview();
                break;
            case OPEN_STATS:
                openStats();
                break;
            case OPEN_MANAGE_BLOCKING:
                openManageBlocking();
                break;
            case OPEN_SETTINGS:
                effects.setValue(ProfileEffect.startActivity(SettingsActivity.class));
                break;
            case OPEN_FAQ:
                effects.setValue(ProfileEffect.startActivity(FAQActivity.class));
                break;
            case OPEN_ABOUT:
                effects.setValue(ProfileEffect.startActivity(AboutActivity.class));
                break;
            case OPEN_DEV_LAB:
                effects.setValue(ProfileEffect.showDevLabConfirm());
                break;
            case BLOCKING_EXTERNAL_RESULT:
                onBlockingExternalResult(intent.blockingSuccess);
                break;
            default:
                break;
        }
    }

    /** 从子页返回或权限结果后整页刷新。 */
    @MainThread
    public void refresh() {
        User user = sessionRepository.getCurrentUser();
        publishState(user);
        if (user != null) {
            loadAvatarAsync(user.avatarPath);
        } else {
            clearAvatar();
        }
        loadStatistics();
    }

    /** Enabler / 快捷方式改 prefs 后回刷屏蔽态（不清 busy 之外的统计缓存）。 */
    @MainThread
    public void onBlockingExternalChanged() {
        blockingBusy = false;
        publishState(sessionRepository.getCurrentUser());
    }

    @MainThread
    private void onBlockingExternalResult(boolean success) {
        blockingBusy = false;
        publishState(sessionRepository.getCurrentUser());
        if (success) {
            effects.setValue(ProfileEffect.showToast(R.string.blocking_toast_enabled, false));
        } else {
            effects.setValue(ProfileEffect.showToast(R.string.blocking_toast_permission_failed, true));
        }
    }

    @MainThread
    private void onToggleBlocking(boolean on) {
        if (blockingBusy) {
            return;
        }
        if (on) {
            if (!sessionRepository.isLoggedIn()) {
                // Switch 已被用户手势拨到开：立刻回刷状态，避免 AuthGate 关闭后仍显示开启。
                publishState(sessionRepository.getCurrentUser());
                effects.setValue(ProfileEffect.requireAuth());
                return;
            }
            blockingBusy = true;
            publishState(sessionRepository.getCurrentUser());
            effects.setValue(ProfileEffect.requestEnableBlocking());
        } else {
            userAppBlockingRepository.setEnabledForCurrentUser(false);
            AppBlockingServiceUtils.stopStandaloneBlocking(application);
            blockingBusy = false;
            publishState(sessionRepository.getCurrentUser());
        }
    }

    @MainThread
    private void openAvatarPreview() {
        ProfileUiState state = uiState.getValue();
        if (state == null || !state.hasAvatar || state.avatarPath == null || state.avatarPath.isEmpty()) {
            return;
        }
        effects.setValue(ProfileEffect.openImagePreview(state.avatarPath));
    }

    @MainThread
    private void openStats() {
        ProfileUiState state = uiState.getValue();
        if (state == null || !state.statsClickable) {
            return;
        }
        effects.setValue(ProfileEffect.navigateToStatistics());
    }

    @MainThread
    private void openManageBlocking() {
        ProfileUiState state = uiState.getValue();
        if (state == null || !state.manageBlockingEnabled) {
            return;
        }
        effects.setValue(ProfileEffect.startActivity(AppBlockingManagementActivity.class));
    }

    private void onActiveUserChanged(@Nullable User user) {
        // 切号：保留旧数字直到新档案统计返回，避免闪零；无档案则清零。
        if (user == null) {
            cachedTotalCompletedCount = 0;
            cachedTotalFocusDurationMs = 0L;
            statsReady = true;
            statsUserId = null;
            blockingBusy = false;
            publishState(null);
            clearAvatar();
            return;
        }
        if (statsUserId == null || !statsUserId.equals(user.userId)) {
            statsReady = false;
            statsUserId = user.userId;
        }
        publishState(user);
        loadAvatarAsync(user.avatarPath);
        loadStatistics();
    }

    private void loadStatistics() {
        if (!sessionRepository.hasActiveProfile()) {
            statsGeneration.incrementAndGet();
            cachedTotalCompletedCount = 0;
            cachedTotalFocusDurationMs = 0L;
            statsReady = true;
            statsUserId = null;
            publishState(sessionRepository.getCurrentUser());
            return;
        }
        final User user = sessionRepository.getCurrentUser();
        if (user == null) {
            return;
        }
        final String expectedUserId = user.userId;
        final int generation = statsGeneration.incrementAndGet();
        statisticsRepository.getProfileSummary((count, durationMs) -> {
            if (generation != statsGeneration.get()) {
                return;
            }
            User current = sessionRepository.getCurrentUser();
            if (current == null || !expectedUserId.equals(current.userId)) {
                return;
            }
            cachedTotalCompletedCount = count;
            cachedTotalFocusDurationMs = durationMs;
            statsReady = true;
            statsUserId = expectedUserId;
            publishState(current);
        });
    }

    @MainThread
    private void publishState(@Nullable User user) {
        boolean hasAllPermissions = PermissionUtils.hasAllAppBlockingPermissions(application);
        boolean prefsEnabled = userAppBlockingRepository.isEnabledForCurrentUser();

        if (prefsEnabled && !hasAllPermissions) {
            userAppBlockingRepository.setEnabledForCurrentUser(false);
            AppBlockingServiceUtils.stopStandaloneBlocking(application);
            prefsEnabled = false;
            blockingBusy = false;
        }

        boolean checked = (hasAllPermissions && prefsEnabled) || blockingBusy;
        int descriptionRes;
        if (hasAllPermissions && prefsEnabled) {
            descriptionRes = R.string.blocking_description_enabled;
        } else if (!hasAllPermissions) {
            descriptionRes = R.string.blocking_description_need_permission;
        } else {
            descriptionRes = R.string.blocking_description_default;
        }

        String nickname;
        String idLabel;
        String signatureText;
        boolean hasSignature;
        String avatarPath;
        boolean hasAvatar;
        boolean accountEnabled;
        boolean manageEnabled;
        boolean statsClickable;
        boolean loggedIn = user != null;
        if (user == null) {
            nickname = application.getString(R.string.profile_login_prompt);
            idLabel = application.getString(R.string.profile_guest_label);
            signatureText = application.getString(R.string.profile_signature_placeholder);
            hasSignature = false;
            avatarPath = null;
            hasAvatar = false;
            accountEnabled = true;
            manageEnabled = false;
            statsClickable = false;
        } else {
            nickname = user.nickname != null && !user.nickname.isEmpty()
                    ? user.nickname
                    : application.getString(R.string.profile_login_prompt);
            idLabel = application.getString(R.string.profile_id_format, user.userId);
            String rawSignature = user.signature;
            hasSignature = rawSignature != null && !rawSignature.trim().isEmpty();
            signatureText = hasSignature
                    ? rawSignature.trim()
                    : application.getString(R.string.profile_signature_placeholder);
            avatarPath = user.avatarPath;
            hasAvatar = avatarPath != null && !avatarPath.isEmpty();
            accountEnabled = true;
            manageEnabled = loggedIn;
            statsClickable = loggedIn;
        }

        boolean showStats = statsReady || user == null;

        uiState.setValue(new ProfileUiState(
                nickname,
                idLabel,
                signatureText,
                hasSignature,
                avatarPath,
                hasAvatar,
                accountEnabled,
                cachedTotalCompletedCount,
                cachedTotalFocusDurationMs,
                showStats,
                statsClickable,
                checked,
                descriptionRes,
                !blockingBusy,
                blockingBusy,
                manageEnabled,
                true
        ));
    }

    private void loadAvatarAsync(@Nullable String avatarPath) {
        final int generation = avatarGeneration.incrementAndGet();
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
                        if (circular != decoded) {
                            decoded.recycle();
                        }
                    }
                }
            } catch (Exception e) {
                AppLog.w(TAG, "Failed to decode avatar", e);
            }
            final Bitmap result = circular;
            mainHandler.post(() -> {
                if (generation != avatarGeneration.get()) {
                    if (result != null && !result.isRecycled()) {
                        result.recycle();
                    }
                    return;
                }
                Bitmap previous = lastDeliveredAvatar;
                lastDeliveredAvatar = result;
                avatarImage.setValue(new ProfileAvatarImage(path, result));
                if (previous != null && previous != result && !previous.isRecycled()) {
                    // 推迟回收，避免 ImageView 仍持有旧 Bitmap 时立刻 recycle。
                    mainHandler.post(() -> {
                        if (!previous.isRecycled()) {
                            previous.recycle();
                        }
                    });
                }
            });
        });
    }

    private void clearAvatar() {
        avatarGeneration.incrementAndGet();
        clearAvatarBitmapDelivery();
        avatarImage.setValue(ProfileAvatarImage.none());
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
    protected void onCleared() {
        statsGeneration.incrementAndGet();
        avatarGeneration.incrementAndGet();
        sessionRepository.getActiveUser().removeObserver(activeUserObserver);
        sessionRepository.getSessionVersion().removeObserver(sessionVersionObserver);
        Bitmap previous = lastDeliveredAvatar;
        lastDeliveredAvatar = null;
        if (previous != null && !previous.isRecycled()) {
            previous.recycle();
        }
        super.onCleared();
    }
}
