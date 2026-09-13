package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.ui.SubpageActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;

public class LoginActivity extends SubpageActivity {
    private LoginViewModel viewModel;

    private TextInputLayout tilUserId;
    private TextInputLayout tilPassword;
    private TextInputEditText etUserId;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton tvGoRegister;
    private MaterialButton tvGoRecover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_login, R.string.title_login);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(LoginViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews() {
        tilUserId = findViewById(R.id.til_user_id);
        tilPassword = findViewById(R.id.til_password);
        etUserId = findViewById(R.id.et_user_id);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);
        tvGoRecover = findViewById(R.id.tv_go_recover);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(this, loading -> {
            boolean inProgress = Boolean.TRUE.equals(loading);
            btnLogin.setEnabled(!inProgress);
            btnLogin.setText(inProgress
                    ? getString(R.string.account_btn_logging_in)
                    : getString(R.string.account_btn_login));
        });
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getLoginSuccess().observe(this, unused -> {
            Toast.makeText(this, R.string.account_toast_login_success, Toast.LENGTH_SHORT).show();
            finish();
        });
        viewModel.getGuardPrompt().observe(this, state -> showBlockingGuardDialog());
        viewModel.getFieldError().observe(this, this::applyFieldError);
    }

    private void applyFieldError(FormFieldError error) {
        if (error == null) {
            return;
        }
        clearFieldErrors();
        switch (error.field) {
            case FormFieldError.FIELD_USER_ID:
                tilUserId.setError(error.message);
                etUserId.requestFocus();
                break;
            case FormFieldError.FIELD_PASSWORD:
                tilPassword.setError(error.message);
                etPassword.requestFocus();
                break;
            default:
                break;
        }
    }

    private void clearFieldErrors() {
        tilUserId.setError(null);
        tilPassword.setError(null);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> submitLogin());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin();
                return true;
            }
            return false;
        });

        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        tvGoRecover.setOnClickListener(v ->
                startActivity(new Intent(this, AccountRecoveryActivity.class)));
    }

    private void submitLogin() {
        clearFieldErrors();
        viewModel.login(
                textOf(etUserId),
                textOf(etPassword));
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private void showBlockingGuardDialog() {
        ModernPromptDialog.builder(this)
                .icon(R.drawable.ic_permission)
                .accent(ModernPromptDialog.Accent.BRAND)
                .title(R.string.account_guard_blocking_title)
                .message(R.string.account_guard_blocking_message)
                .primary(R.string.account_guard_disable_blocking_continue,
                        () -> viewModel.continueAfterDisablingBlocking())
                .tertiary(R.string.account_guard_cancel_operation, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
