package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.R;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class SetNewPasswordActivity extends AppCompatActivity {
    private SetNewPasswordViewModel viewModel;

    private ImageView btnBack;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnSubmit;
    private TextView tvTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_new_password);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(SetNewPasswordViewModel.class);

        btnBack = findViewById(R.id.btn_back);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSubmit = findViewById(R.id.btn_submit);
        tvTip = findViewById(R.id.tv_tip);

        btnBack.setOnClickListener(v -> showConfirmExitDialog());
        btnSubmit.setOnClickListener(v -> viewModel.updatePassword(
                etNewPassword.getText().toString(),
                etConfirmPassword.getText().toString()));

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
        switch (error.field) {
            case FormFieldError.FIELD_NEW_PASSWORD:
                etNewPassword.setError(error.message);
                etNewPassword.requestFocus();
                break;
            case FormFieldError.FIELD_CONFIRM_PASSWORD:
                etConfirmPassword.setError(error.message);
                etConfirmPassword.requestFocus();
                break;
            case FormFieldError.FIELD_TIP:
                tvTip.setText(error.message);
                tvTip.setVisibility(TextView.VISIBLE);
                etNewPassword.requestFocus();
                break;
            default:
                break;
        }
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
        new AlertDialog.Builder(this)
            .setTitle(R.string.common_dialog_hint_title)
            .setMessage(R.string.account_dialog_force_password_message)
            .setNegativeButton(R.string.account_dialog_force_password_continue, null)
            .setPositiveButton(R.string.account_dialog_force_password_exit,
                    (dialog, which) -> viewModel.logoutOnExit())
            .show();
    }
}
