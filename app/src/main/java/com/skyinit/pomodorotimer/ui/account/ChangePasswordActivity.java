package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.data.model.FormFieldError;
import com.skyinit.pomodorotimer.R;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;

public class ChangePasswordActivity extends BaseActivity {
    private ChangePasswordViewModel viewModel;

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnSubmit;
    private TextView tvTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_change_password));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(ChangePasswordViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews() {
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
        switch (error.field) {
            case FormFieldError.FIELD_OLD_PASSWORD:
                etOldPassword.setError(error.message);
                etOldPassword.requestFocus();
                break;
            case FormFieldError.FIELD_NEW_PASSWORD:
                etNewPassword.requestFocus();
                break;
            case FormFieldError.FIELD_CONFIRM_PASSWORD:
                etConfirmPassword.setError(error.message);
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

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> viewModel.updatePassword(
                etOldPassword.getText().toString(),
                etNewPassword.getText().toString(),
                etConfirmPassword.getText().toString()));
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
