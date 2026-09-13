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

public class AccountRecoveryActivity extends SubpageActivity {
    private AccountRecoveryViewModel viewModel;

    private TextInputLayout tilUserId;
    private TextInputLayout tilNickname;
    private TextInputEditText etUserId;
    private TextInputEditText etNickname;
    private MaterialButton btnRecover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_account_recovery, R.string.title_account_recovery);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(AccountRecoveryViewModel.class);

        tilUserId = findViewById(R.id.til_user_id);
        tilNickname = findViewById(R.id.til_nickname);
        etUserId = findViewById(R.id.et_user_id);
        etNickname = findViewById(R.id.et_nickname);
        btnRecover = findViewById(R.id.btn_recover);

        btnRecover.setOnClickListener(v -> {
            clearFieldErrors();
            viewModel.recover(textOf(etUserId), textOf(etNickname));
        });

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
            case FormFieldError.FIELD_NICKNAME:
                tilNickname.setError(error.message);
                etNickname.requestFocus();
                break;
            default:
                break;
        }
    }

    private void clearFieldErrors() {
        tilUserId.setError(null);
        tilNickname.setError(null);
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
