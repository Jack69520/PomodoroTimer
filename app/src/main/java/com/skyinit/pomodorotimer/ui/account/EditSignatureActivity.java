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
 * 修改签名页。
 */
public class EditSignatureActivity extends SubpageActivity {

    private EditSignatureViewModel viewModel;
    private TextInputEditText signatureInput;
    private MaterialButton btnSave;
    private boolean seeded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_edit_signature, R.string.title_edit_signature);

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(EditSignatureViewModel.class);

        signatureInput = findViewById(R.id.signature_input);
        btnSave = findViewById(R.id.btn_save_signature);
        btnSave.setOnClickListener(v -> {
            CharSequence text = signatureInput.getText();
            viewModel.save(text != null ? text.toString() : "");
        });

        viewModel.getSignature().observe(this, value -> {
            if (!seeded) {
                seeded = true;
                String seed = value != null ? value : "";
                signatureInput.setText(seed);
                signatureInput.setSelection(seed.length());
            }
        });
        viewModel.getSaving().observe(this, saving -> {
            boolean busy = saving != null && saving;
            btnSave.setEnabled(!busy);
            signatureInput.setEnabled(!busy);
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
