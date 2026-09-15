package com.skyinit.pomodorotimer.ui.account;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.AvatarStorage;
import com.skyinit.pomodorotimer.data.model.ProfileAvatarImage;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;
import com.skyinit.pomodorotimer.ui.profile.ImagePreviewActivity;
import com.skyinit.pomodorotimer.util.AppLog;

import java.io.File;

/**
 * 个人资料页：资料编辑 + 底部会话快捷操作（退出 / 切号 1:1）。
 */
public class AccountActivity extends SubpageActivity {

    private static final String TAG = "AccountActivity";

    private AccountViewModel viewModel;
    @Nullable
    private String currentAvatarPath;
    @Nullable
    private Uri pendingCameraUri;

    private View sectionProfile;
    private View rowAvatar;
    private ImageView rowAvatarPreview;
    private TextView rowUserIdValue;
    private View rowNickname;
    private TextView rowNicknameValue;
    private View rowSignature;
    private TextView rowSignatureValue;
    private View sectionRegisteredActions;
    private MaterialButton btnLogout;
    private MaterialButton btnSwitchAccount;
    private View sectionGuestActions;
    private MaterialButton btnRegister;
    private MaterialButton btnLogin;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    viewModel.dispatch(AccountIntent.avatarUriSelected(uri));
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                Uri uri = pendingCameraUri;
                pendingCameraUri = null;
                if (Boolean.TRUE.equals(success) && uri != null) {
                    viewModel.dispatch(AccountIntent.avatarUriSelected(uri));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_account, R.string.title_account);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(AccountViewModel.class);

        bindViews();
        setupClicks();
        observeViewModel();
    }

    private void bindViews() {
        sectionProfile = findViewById(R.id.section_profile);
        rowAvatar = findViewById(R.id.row_avatar);
        rowAvatarPreview = findViewById(R.id.row_avatar_preview);
        rowUserIdValue = findViewById(R.id.row_user_id_value);
        rowNickname = findViewById(R.id.row_nickname);
        rowNicknameValue = findViewById(R.id.row_nickname_value);
        rowSignature = findViewById(R.id.row_signature);
        rowSignatureValue = findViewById(R.id.row_signature_value);
        sectionRegisteredActions = findViewById(R.id.section_registered_actions);
        btnLogout = findViewById(R.id.btn_logout);
        btnSwitchAccount = findViewById(R.id.btn_switch_account);
        sectionGuestActions = findViewById(R.id.section_guest_actions);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void setupClicks() {
        rowAvatar.setOnClickListener(v -> viewModel.dispatch(AccountIntent.openAvatar()));
        rowNickname.setOnClickListener(v -> viewModel.dispatch(AccountIntent.openNickname()));
        rowSignature.setOnClickListener(v -> viewModel.dispatch(AccountIntent.openSignature()));
        btnLogout.setOnClickListener(v -> viewModel.dispatch(AccountIntent.logout()));
        btnSwitchAccount.setOnClickListener(v -> viewModel.dispatch(AccountIntent.switchAccount()));
        btnRegister.setOnClickListener(v -> viewModel.dispatch(AccountIntent.upgradeRegister()));
        btnLogin.setOnClickListener(v -> viewModel.dispatch(AccountIntent.login()));
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getAvatarImage().observe(this, this::bindAvatar);
        viewModel.getEffects().observe(this, this::handleEffect);
    }

    private void render(@Nullable AccountUiState state) {
        if (state == null || isFinishing() || isDestroyed()) {
            return;
        }

        rowUserIdValue.setText(state.idDisplay);
        rowNicknameValue.setText(state.nickname);
        rowSignatureValue.setText(state.signatureDisplay);
        rowSignatureValue.setAlpha(state.hasCustomSignature ? 1f : 0.65f);

        currentAvatarPath = state.hasAvatar ? state.avatarPath : null;
        if (!state.hasAvatar) {
            rowAvatarPreview.setImageResource(R.drawable.ic_default_avatar);
        }

        int registeredVis = state.registered ? View.VISIBLE : View.GONE;
        int guestVis = state.registered ? View.GONE : View.VISIBLE;
        sectionProfile.setVisibility(registeredVis);
        sectionRegisteredActions.setVisibility(registeredVis);
        sectionGuestActions.setVisibility(guestVis);

        boolean enabled = !state.actionInProgress;
        rowAvatar.setEnabled(enabled);
        rowNickname.setEnabled(enabled);
        rowSignature.setEnabled(enabled);
        btnLogout.setEnabled(enabled);
        btnSwitchAccount.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnLogin.setEnabled(enabled);
    }

    private void bindAvatar(@Nullable ProfileAvatarImage image) {
        if (rowAvatarPreview == null) {
            return;
        }
        if (image == null || image.bitmap == null) {
            if (currentAvatarPath == null) {
                rowAvatarPreview.setImageResource(R.drawable.ic_default_avatar);
            }
            return;
        }
        if (currentAvatarPath == null || !currentAvatarPath.equals(image.path)) {
            return;
        }
        rowAvatarPreview.setImageBitmap(image.bitmap);
    }

    private void handleEffect(@Nullable AccountEffect effect) {
        if (effect == null || isFinishing() || isDestroyed()) {
            return;
        }
        switch (effect.type) {
            case SHOW_TOAST:
                Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
                break;
            case SHOW_TOAST_TEXT:
                if (effect.toastText != null) {
                    Toast.makeText(this, effect.toastText, Toast.LENGTH_SHORT).show();
                }
                break;
            case SHOW_AVATAR_ACTIONS:
                showAvatarActionsDialog();
                break;
            case SHOW_PICK_AVATAR:
                showPickAvatarDialog();
                break;
            case SHOW_LOGOUT_CONFIRM:
                ModernPromptDialog.builder(this)
                        .icon(R.drawable.ic_info)
                        .accent(ModernPromptDialog.Accent.BRAND)
                        .title(R.string.account_dialog_logout_title)
                        .message(R.string.account_dialog_logout_message)
                        .primary(R.string.account_dialog_logout_confirm,
                                () -> viewModel.dispatch(AccountIntent.confirmLogout()))
                        .tertiary(R.string.cancel, null)
                        .show();
                break;
            case SHOW_GUARD_PROMPT:
                ModernPromptDialog.builder(this)
                        .icon(R.drawable.ic_permission)
                        .accent(ModernPromptDialog.Accent.BRAND)
                        .title(R.string.account_guard_blocking_title)
                        .message(R.string.account_guard_blocking_message)
                        .primary(R.string.account_guard_disable_blocking_continue,
                                () -> viewModel.dispatch(
                                        AccountIntent.continueAfterDisablingBlocking()))
                        .tertiary(R.string.account_guard_cancel_operation, null)
                        .show();
                break;
            case START_ACTIVITY:
                if (effect.activityClass != null) {
                    startActivity(new Intent(this, effect.activityClass));
                }
                break;
            case OPEN_IMAGE_PREVIEW:
                if (effect.imagePath != null) {
                    Intent intent = new Intent(this, ImagePreviewActivity.class);
                    intent.putExtra("image_path", effect.imagePath);
                    startActivity(intent);
                }
                break;
            case OPEN_GALLERY:
                galleryLauncher.launch("image/*");
                break;
            case OPEN_CAMERA:
                launchCamera();
                break;
            default:
                break;
        }
    }

    private void showAvatarActionsDialog() {
        String[] options = getResources().getStringArray(R.array.account_avatar_options);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_dialog_avatar_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.dispatch(AccountIntent.viewAvatar());
                    } else if (which == 1) {
                        viewModel.dispatch(AccountIntent.changeAvatar());
                    }
                })
                .show();
    }

    private void showPickAvatarDialog() {
        String[] options = getResources().getStringArray(R.array.account_pick_avatar_options);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_dialog_pick_avatar_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.dispatch(AccountIntent.pickFromGallery());
                    } else if (which == 1) {
                        viewModel.dispatch(AccountIntent.takePhoto());
                    }
                })
                .show();
    }

    private void launchCamera() {
        try {
            File temp = AvatarStorage.getInstance().createCameraCaptureFile(this);
            Uri photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", temp);
            pendingCameraUri = photoUri;
            cameraLauncher.launch(photoUri);
        } catch (Exception e) {
            AppLog.w(TAG, "Camera unavailable", e);
            pendingCameraUri = null;
            Toast.makeText(this, R.string.account_toast_camera_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
