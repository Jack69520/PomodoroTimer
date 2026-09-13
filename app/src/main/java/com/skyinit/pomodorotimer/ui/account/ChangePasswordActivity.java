package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.ui.SubpageActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends SubpageActivity {
    private ChangePasswordViewModel viewModel;

    private TextInputLayout tilOldPassword;
    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etOldPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnSubmit;
    private TextView tvTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_change_password, R.string.title_change_password);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(ChangePasswordViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews() {
        tilOldPassword = findViewById(R.id.til_old_password);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSubmit = findViewById(R.id.btn_submit);
        tvTip = findViewById(R.id.tv_tip);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(this, loading -> {
            boolean inProgress = Boolean.TRUE.equals(loading);
            btnSubmit.setEnabled(!inProgress);
            btnSubmit.setText(inProgress
                    ? getString(R.string.account_btn_submitting)
                    : getString(R.string.account_btn_change_password));
        });
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getUpdateSuccess().observe(this, unused -> finish());
        viewModel.getFieldError().observe(this, this::applyFieldError);
    }

    private void applyFieldError(FormFieldError error) {
        if (error == null) {
            return;
        }
        clearFieldErrors();
        tvTip.setVisibility(View.GONE);
        switch (error.field) {
            case FormFieldError.FIELD_OLD_PASSWORD:
                tilOldPassword.setError(error.message);
                etOldPassword.requestFocus();
                break;
            case FormFieldError.FIELD_NEW_PASSWORD:
                tilNewPassword.setError(error.message);
                etNewPassword.requestFocus();
                break;
            case FormFieldError.FIELD_CONFIRM_PASSWORD:
                tilConfirmPassword.setError(error.message);
                etConfirmPassword.requestFocus();
                break;
            case FormFieldError.FIELD_TIP:
                tvTip.setVisibility(View.VISIBLE);
                tvTip.setText(error.message);
                etNewPassword.requestFocus();
                break;
            default:
                break;
        }
    }

    private void clearFieldErrors() {
        tilOldPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> {
            clearFieldErrors();
            tvTip.setVisibility(View.GONE);
            viewModel.updatePassword(
                    textOf(etOldPassword),
                    textOf(etNewPassword),
                    textOf(etConfirmPassword));
        });
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
