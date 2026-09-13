package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.ui.SubpageActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;

public class RegisterActivity extends SubpageActivity {
    private RegisterViewModel viewModel;

    private TextInputLayout tilNickname;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etNickname;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputEditText etSignature;
    private MaterialButton btnRegister;
    private MaterialButton tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_register, R.string.title_register);

        App app = (App) getApplication();
        if (app.getContainer().getUserSessionRepository().isLoggedIn()) {
            ModernPromptDialog.builder(this)
                    .icon(R.drawable.ic_lock)
                    .accent(ModernPromptDialog.Accent.BRAND)
                    .title(R.string.auth_gate_title)
                    .message(R.string.account_error_logout_before_register)
                    .primary(R.string.confirm, this::finish)
                    .cancelable(true)
                    .show()
                    .setOnCancelListener(d -> finish());
            return;
        }
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(RegisterViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews() {
        tilNickname = findViewById(R.id.til_nickname);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etNickname = findViewById(R.id.et_nickname);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etSignature = findViewById(R.id.et_signature);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);
    }

    private void observeViewModel() {
        viewModel.getInitialNickname().observe(this, nickname -> {
            if (nickname != null && etNickname.getText() != null && etNickname.getText().length() == 0) {
                etNickname.setText(nickname);
            }
        });
        viewModel.isLoading().observe(this, loading -> {
            boolean inProgress = Boolean.TRUE.equals(loading);
            btnRegister.setEnabled(!inProgress);
            btnRegister.setText(inProgress
                    ? getString(R.string.account_btn_registering)
                    : getString(R.string.account_btn_register));
        });
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getRegisterSuccess().observe(this, unused -> finish());
        viewModel.getFieldError().observe(this, this::applyFieldError);
    }

    private void applyFieldError(FormFieldError error) {
        if (error == null) {
            return;
        }
        clearFieldErrors();
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

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> {
            clearFieldErrors();
            viewModel.register(
                    textOf(etNickname),
                    textOf(etPassword),
                    textOf(etConfirmPassword),
                    textOf(etSignature));
        });

        tvGoLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
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
