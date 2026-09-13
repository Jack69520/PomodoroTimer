package com.skyinit.pomodorotimer.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.data.repository.FirstRunRepository;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;
import com.skyinit.pomodorotimer.util.ShortcutActions;

/**
 * 首次启动注册页：无 TopBar，右上角可跳过。
 */
public class FirstRegisterActivity extends BaseActivity {

    private FirstRegisterViewModel viewModel;
    private TextInputLayout tilNickname;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etNickname;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnRegister;
    private MaterialButton btnSkip;

    @Override
    protected void applyThemeStyle() {
        setTheme(R.style.Theme_PomodoroTimer_NoActionBar);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirstRunRepository firstRun = FirstRunRepository.getInstance(this);
        if (firstRun.isAuthFlowCompleted()) {
            navigateForward();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_first_register);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(FirstRegisterViewModel.class);

        tilNickname = findViewById(R.id.til_nickname);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etNickname = findViewById(R.id.et_nickname);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_first_register);
        btnSkip = findViewById(R.id.btn_first_register_skip);
        applyWindowInsets();

        btnRegister.setOnClickListener(v -> {
            clearFieldErrors();
            viewModel.register(
                    textOf(etNickname),
                    textOf(etPassword),
                    textOf(etConfirmPassword));
        });
        btnSkip.setOnClickListener(v -> showSkipDialog());

        observeViewModel();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showSkipDialog();
            }
        });
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(this, loading -> {
            boolean inProgress = Boolean.TRUE.equals(loading);
            btnRegister.setEnabled(!inProgress);
            btnSkip.setEnabled(!inProgress);
            btnRegister.setText(inProgress
                    ? getString(R.string.account_btn_registering)
                    : getString(R.string.account_btn_register));
        });
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getFieldError().observe(this, this::applyFieldError);
        viewModel.getRegisterSuccessUserId().observe(this, userId -> {
            if (userId == null) {
                return;
            }
            Toast.makeText(this, getString(R.string.account_register_success, userId),
                    Toast.LENGTH_LONG).show();
            FirstRunRepository.getInstance(this).markAuthFlowCompleted();
            navigateForward();
        });
    }

    private void applyFieldError(FormFieldError error) {
        if (error == null) {
            return;
        }
        switch (error.field) {
            case FormFieldError.FIELD_NICKNAME:
                tilNickname.setError(error.message);
                etNickname.requestFocus();
                break;
            case FormFieldError.FIELD_PASSWORD:
                tilPassword.setError(error.message);
                etPassword.requestFocus();
                break;
            case FormFieldError.FIELD_CONFIRM_PASSWORD:
                tilConfirmPassword.setError(error.message);
                etConfirmPassword.requestFocus();
                break;
            default:
                break;
        }
    }

    private void clearFieldErrors() {
        tilNickname.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void showSkipDialog() {
        ModernPromptDialog.builder(this)
                .icon(R.drawable.ic_info)
                .accent(ModernPromptDialog.Accent.BRAND)
                .title(R.string.first_register_skip_dialog_title)
                .message(R.string.first_register_skip_dialog_message)
                .primary(R.string.first_register_skip_confirm, () -> {
                    FirstRunRepository.getInstance(this).markAuthFlowCompleted();
                    navigateForward();
                })
                .tertiary(R.string.cancel, null)
                .show();
    }

    private void navigateForward() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        if (FirstRunRepository.getInstance(this).isOnboardingCompleted()) {
            intent = new Intent(this, com.skyinit.pomodorotimer.MainActivity.class);
        }
        ShortcutActions.copyShortcutAction(getIntent(), intent);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.first_register_root);
        View scroll = findViewById(R.id.first_register_scroll);
        if (root == null || btnSkip == null || scroll == null) {
            return;
        }
        final int skipPadL = btnSkip.getPaddingLeft();
        final int skipPadT = btnSkip.getPaddingTop();
        final int skipPadR = btnSkip.getPaddingRight();
        final int skipPadB = btnSkip.getPaddingBottom();
        final int scrollPadL = scroll.getPaddingLeft();
        final int scrollPadT = scroll.getPaddingTop();
        final int scrollPadR = scroll.getPaddingRight();
        final int scrollPadB = scroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            int top = Math.max(statusBars.top, cutout.top);
            int left = Math.max(navigationBars.left, cutout.left);
            int right = Math.max(navigationBars.right, cutout.right);
            int bottom = Math.max(navigationBars.bottom, ime.bottom);

            btnSkip.setPadding(skipPadL + left, skipPadT + top, skipPadR + right, skipPadB);
            scroll.setPadding(scrollPadL + left, scrollPadT, scrollPadR + right, scrollPadB + bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private static String textOf(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString();
    }
}
