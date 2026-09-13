package com.skyinit.pomodorotimer.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;

/**
 * 账户与安全：修改密码、退出登录、登录其他账户、注销账户。
 */
public class AccountSecurityActivity extends SubpageActivity {

    private AccountSecurityViewModel viewModel;

    private View sectionRegistered;
    private View sectionGuest;
    private View rowChangePassword;
    private View rowLogout;
    private View rowSwitchAccount;
    private View rowDeleteAccount;
    private MaterialButton btnRegister;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_account_security, R.string.title_account_security);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(AccountSecurityViewModel.class);

        bindViews();
        setupClicks();
        observeViewModel();
    }

    private void bindViews() {
        sectionRegistered = findViewById(R.id.section_registered);
        sectionGuest = findViewById(R.id.section_guest);
        rowChangePassword = findViewById(R.id.row_change_password);
        rowLogout = findViewById(R.id.row_logout);
        rowSwitchAccount = findViewById(R.id.row_switch_account);
        rowDeleteAccount = findViewById(R.id.row_delete_account);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void setupClicks() {
        rowChangePassword.setOnClickListener(
                v -> viewModel.dispatch(AccountSecurityIntent.changePassword()));
        rowLogout.setOnClickListener(
                v -> viewModel.dispatch(AccountSecurityIntent.logout()));
        rowSwitchAccount.setOnClickListener(
                v -> viewModel.dispatch(AccountSecurityIntent.switchAccount()));
        rowDeleteAccount.setOnClickListener(
                v -> viewModel.dispatch(AccountSecurityIntent.deleteAccount()));
        btnRegister.setOnClickListener(
                v -> viewModel.dispatch(AccountSecurityIntent.upgradeRegister()));
        btnLogin.setOnClickListener(
                v -> viewModel.dispatch(AccountSecurityIntent.login()));
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::render);
        viewModel.getEffects().observe(this, this::handleEffect);
    }

    private void render(@Nullable AccountSecurityUiState state) {
        if (state == null || isFinishing() || isDestroyed()) {
            return;
        }
        int registeredVis = state.registered ? View.VISIBLE : View.GONE;
        int guestVis = state.registered ? View.GONE : View.VISIBLE;
        sectionRegistered.setVisibility(registeredVis);
        sectionGuest.setVisibility(guestVis);

        boolean enabled = !state.actionInProgress;
        rowChangePassword.setEnabled(enabled);
        rowLogout.setEnabled(enabled);
        rowSwitchAccount.setEnabled(enabled);
        rowDeleteAccount.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnLogin.setEnabled(enabled);
    }

    private void handleEffect(@Nullable AccountSecurityEffect effect) {
        if (effect == null || isFinishing() || isDestroyed()) {
            return;
        }
        switch (effect.type) {
            case SHOW_TOAST:
                Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
                break;
            case SHOW_TOAST_TEXT:
                if (effect.toastText != null && !effect.toastText.isEmpty()) {
                    Toast.makeText(this, effect.toastText, Toast.LENGTH_SHORT).show();
                }
                break;
            case SHOW_LOGOUT_CONFIRM:
                ModernPromptDialog.builder(this)
                        .icon(R.drawable.ic_info)
                        .accent(ModernPromptDialog.Accent.BRAND)
                        .title(R.string.account_dialog_logout_title)
                        .message(R.string.account_dialog_logout_message)
                        .primary(R.string.account_dialog_logout_confirm,
                                () -> viewModel.dispatch(AccountSecurityIntent.confirmLogout()))
                        .tertiary(R.string.cancel, null)
                        .show();
                break;
            case SHOW_DELETE_CONFIRM:
                ModernPromptDialog.builder(this)
                        .icon(R.drawable.ic_info)
                        .accent(ModernPromptDialog.Accent.DANGER)
                        .title(R.string.account_dialog_delete_title)
                        .message(R.string.account_dialog_delete_message)
                        .primaryDanger(R.string.account_dialog_delete_confirm,
                                () -> viewModel.dispatch(AccountSecurityIntent.confirmDelete()))
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
                                        AccountSecurityIntent.continueAfterDisablingBlocking()))
                        .tertiary(R.string.account_guard_cancel_operation, null)
                        .show();
                break;
            case SHOW_DELETE_SUCCESS:
                ModernPromptDialog.builder(this)
                        .icon(R.drawable.ic_check)
                        .accent(ModernPromptDialog.Accent.SUCCESS)
                        .title(R.string.account_delete_success_title)
                        .message(R.string.account_delete_success_message)
                        .primary(R.string.confirm, this::finish)
                        .cancelable(false)
                        .show();
                break;
            case START_ACTIVITY:
                if (effect.activityClass != null) {
                    startActivity(new Intent(this, effect.activityClass));
                }
                break;
            case FINISH:
                if (effect.toastRes != 0) {
                    Toast.makeText(this, effect.toastRes, Toast.LENGTH_SHORT).show();
                }
                finish();
                break;
            default:
                break;
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
