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
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;

public class AccountRecoveryActivity extends BaseActivity {
    private AccountRecoveryViewModel viewModel;

    private EditText etUserId;
    private EditText etNickname;
    private Button btnRecover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_recovery);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_account_recovery));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(AccountRecoveryViewModel.class);

        etUserId = findViewById(R.id.et_user_id);
        etNickname = findViewById(R.id.et_nickname);
        btnRecover = findViewById(R.id.btn_recover);

        btnRecover.setOnClickListener(v -> viewModel.recover(
                etUserId.getText().toString(),
                etNickname.getText().toString()));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(this, loading -> {
            boolean inProgress = Boolean.TRUE.equals(loading);
            btnRecover.setEnabled(!inProgress);
            btnRecover.setText(inProgress
                    ? getString(R.string.account_btn_verifying)
                    : getString(R.string.account_btn_set_password));
        });
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getRecoverySuccess().observe(this, unused -> {
            startActivity(new Intent(this, SetNewPasswordActivity.class));
            finish();
        });
        viewModel.getGuardPrompt().observe(this, state -> showBlockingGuardDialog());
        viewModel.getFieldError().observe(this, this::applyFieldError);
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

    private void applyFieldError(FormFieldError error) {
        if (error == null) {
            return;
        }
        switch (error.field) {
            case FormFieldError.FIELD_USER_ID:
                etUserId.setError(error.message);
                etUserId.requestFocus();
                break;
            case FormFieldError.FIELD_NICKNAME:
                etNickname.setError(error.message);
                etNickname.requestFocus();
                break;
            default:
                break;
        }
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
