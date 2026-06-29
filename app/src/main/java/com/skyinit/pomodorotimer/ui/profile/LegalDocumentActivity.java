package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LegalDocumentActivity extends BaseActivity {

    public static final String EXTRA_DOCUMENT_TYPE = "document_type";
    public static final String TYPE_PRIVACY_POLICY = "privacy_policy";
    public static final String TYPE_USER_AGREEMENT = "user_agreement";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_document);

        String documentType = getIntent().getStringExtra(EXTRA_DOCUMENT_TYPE);
        if (documentType == null) {
            finish();
            return;
        }

        int titleRes;
        String assetFileName;
        if (TYPE_USER_AGREEMENT.equals(documentType)) {
            titleRes = R.string.title_user_agreement;
            assetFileName = "user_agreement.txt";
        } else {
            titleRes = R.string.title_privacy_policy;
            assetFileName = "privacy_policy.txt";
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(titleRes));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TextView contentView = findViewById(R.id.tv_legal_content);
        String content = loadAssetText(assetFileName);
        if (content == null) {
            Toast.makeText(this, R.string.legal_document_load_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        contentView.setText(content);
    }

    @Nullable
    private String loadAssetText(String fileName) {
        try (InputStream inputStream = getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
