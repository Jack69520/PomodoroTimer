package com.skyinit.pomodorotimer.ui.account;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;

/**
 * 修改昵称页。
 */
public class EditNicknameActivity extends SubpageActivity {

    private EditNicknameViewModel viewModel;
    private TextInputEditText nicknameInput;
    private MaterialButton btnSave;
    private boolean seeded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_edit_nickname, R.string.title_edit_nickname);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(EditNicknameViewModel.class);

        nicknameInput = findViewById(R.id.nickname_input);
        btnSave = findViewById(R.id.btn_save_nickname);
        btnSave.setOnClickListener(v -> {
            CharSequence text = nicknameInput.getText();
            viewModel.save(text != null ? text.toString() : "");
        });

        viewModel.getNickname().observe(this, value -> {
            if (!seeded && value != null) {
                seeded = true;
                nicknameInput.setText(value);
                nicknameInput.setSelection(value.length());
            }
        });
        viewModel.getSaving().observe(this, saving -> {
            boolean busy = saving != null && saving;
            btnSave.setEnabled(!busy);
            nicknameInput.setEnabled(!busy);
            btnSave.setText(busy ? R.string.account_btn_saving : R.string.account_btn_save);
        });
        viewModel.getToastRes().observe(this, res -> {
            if (res != null) {
                Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getToastText().observe(this, text -> {
            if (text != null) {
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getFinishPage().observe(this, unused -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
