package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.ui.SubpageActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;

public class SetNewPasswordActivity extends SubpageActivity {
    private SetNewPasswordViewModel viewModel;

    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnSubmit;
    private TextView tvTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_set_new_password, R.string.title_set_new_password);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(SetNewPasswordViewModel.class);

        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSubmit = findViewById(R.id.btn_submit);
        tvTip = findViewById(R.id.tv_tip);

        btnSubmit.setOnClickListener(v -> {
            clearFieldErrors();
            tvTip.setVisibility(View.GONE);
            viewModel.updatePassword(textOf(etNewPassword), textOf(etConfirmPassword));
        });

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(this, loading -> {
            boolean inProgress = Boolean.TRUE.equals(loading);
            btnSubmit.setEnabled(!inProgress);
            btnSubmit.setText(inProgress
                    ? getString(R.string.account_btn_submitting)
                    : getString(R.string.account_btn_confirm));
        });
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getUpdateSuccess().observe(this, unused -> finish());
        viewModel.getShouldFinish().observe(this, unused -> finish());
        viewModel.getFieldError().observe(this, this::applyFieldError);
    }

    private void applyFieldError(FormFieldError error) {
        if (error == null) {
            return;
        }
        clearFieldErrors();
        tvTip.setVisibility(View.GONE);
        switch (error.field) {
            case FormFieldError.FIELD_NEW_PASSWORD:
                tilNewPassword.setError(error.message);
                etNewPassword.requestFocus();
                break;
            case FormFieldError.FIELD_CONFIRM_PASSWORD:
                tilConfirmPassword.setError(error.message);
                etConfirmPassword.requestFocus();
                break;
            case FormFieldError.FIELD_TIP:
                tvTip.setText(error.message);
                tvTip.setVisibility(View.VISIBLE);
                etNewPassword.requestFocus();
                break;
            default:
                break;
        }
    }

    private void clearFieldErrors() {
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            showConfirmExitDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        showConfirmExitDialog();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showConfirmExitDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.checkAccessOnResume();
    }

    private void showConfirmExitDialog() {
        ModernPromptDialog.builder(this)
                .icon(R.drawable.ic_lock)
                .accent(ModernPromptDialog.Accent.DANGER)
                .title(R.string.common_dialog_hint_title)
                .message(R.string.account_dialog_force_password_message)
                .primaryDanger(R.string.account_dialog_force_password_exit,
                        () -> viewModel.logoutOnExit())
                .secondary(R.string.account_dialog_force_password_continue, null)
                .show();
    }
}
