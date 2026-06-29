package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.R;
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

public class RegisterActivity extends BaseActivity {
    private RegisterViewModel viewModel;

    private EditText etNickname;
    private EditText etPassword;
    private ImageView ivPasswordToggle;
    private EditText etConfirmPassword;
    private ImageView ivConfirmPasswordToggle;
    private EditText etSignature;
    private Button btnRegister;
    private TextView tvGoLogin;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_register));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(RegisterViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews() {
        etNickname = findViewById(R.id.et_nickname);
        etPassword = findViewById(R.id.et_password);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        ivConfirmPasswordToggle = findViewById(R.id.iv_confirm_password_toggle);
        etSignature = findViewById(R.id.et_signature);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);
    }

    private void observeViewModel() {
        viewModel.getInitialNickname().observe(this, nickname -> {
            if (nickname != null && etNickname.getText().length() == 0) {
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
        switch (error.field) {
            case FormFieldError.FIELD_NICKNAME:
                etNickname.setError(error.message);
                etNickname.requestFocus();
                break;
            case FormFieldError.FIELD_PASSWORD:
                etPassword.setError(error.message);
                etPassword.requestFocus();
                break;
            case FormFieldError.FIELD_CONFIRM_PASSWORD:
                etConfirmPassword.setError(error.message);
                etConfirmPassword.requestFocus();
                break;
            default:
                break;
        }
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> viewModel.register(
                etNickname.getText().toString(),
                etPassword.getText().toString(),
                etConfirmPassword.getText().toString(),
                etSignature.getText().toString()));

        tvGoLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());
        ivConfirmPasswordToggle.setOnClickListener(v -> toggleConfirmPasswordVisibility());
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

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivConfirmPasswordToggle.setImageResource(R.drawable.ic_eye_off);
            isConfirmPasswordVisible = false;
        } else {
            etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivConfirmPasswordToggle.setImageResource(R.drawable.ic_eye_on);
            isConfirmPasswordVisible = true;
        }

        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }
}
