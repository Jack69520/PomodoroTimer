package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.R;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import androidx.lifecycle.ViewModelProvider;

public class LoginActivity extends BaseActivity {
    private LoginViewModel viewModel;

    private EditText etUserId;
    private EditText etPassword;
    private ImageView ivPasswordToggle;
    private Button btnLogin;
    private TextView tvGoRegister;
    private TextView tvGoRecover;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_login));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(LoginViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews() {
        etUserId = findViewById(R.id.et_user_id);
        etPassword = findViewById(R.id.et_password);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);
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
        switch (error.field) {
            case FormFieldError.FIELD_USER_ID:
                etUserId.setError(error.message);
                etUserId.requestFocus();
                break;
            case FormFieldError.FIELD_PASSWORD:
                etPassword.setError(error.message);
                etPassword.requestFocus();
                break;
            default:
                break;
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> viewModel.login(
                etUserId.getText().toString(),
                etPassword.getText().toString()));

        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        tvGoRecover.setOnClickListener(v ->
                startActivity(new Intent(this, AccountRecoveryActivity.class)));

        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void showBlockingGuardDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.account_guard_blocking_title)
                .setMessage(R.string.account_guard_blocking_message)
                .setNegativeButton(R.string.account_guard_cancel_operation, null)
                .setPositiveButton(R.string.account_guard_disable_blocking_continue,
                        (dialog, which) -> viewModel.continueAfterDisablingBlocking())
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

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_off);
            isPasswordVisible = false;
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_on);
            isPasswordVisible = true;
        }

        etPassword.setSelection(etPassword.getText().length());
    }
}
