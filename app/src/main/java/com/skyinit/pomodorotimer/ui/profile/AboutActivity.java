package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_about));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        try {
            TextView appName = findViewById(R.id.tv_app_name);
            appName.setText(getString(R.string.app_name));

            ImageView appIcon = findViewById(R.id.iv_app_icon);
            appIcon.setImageResource(R.mipmap.ic_launcher);

            TextView appVersion = findViewById(R.id.tv_app_version);
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            appVersion.setText(String.format("Version %s", versionName));

            TextView privacyPolicy = findViewById(R.id.tv_privacy_policy);
            privacyPolicy.setOnClickListener(v -> openLegalDocument(LegalDocumentActivity.TYPE_PRIVACY_POLICY));

            TextView userAgreement = findViewById(R.id.tv_user_agreement);
            userAgreement.setOnClickListener(v -> openLegalDocument(LegalDocumentActivity.TYPE_USER_AGREEMENT));

            TextView openSourceLicenses = findViewById(R.id.tv_open_source_licenses);
            openSourceLicenses.setOnClickListener(v -> {
                Intent intent = new Intent(this, OpenSourceLicensesActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openLegalDocument(String documentType) {
        Intent intent = new Intent(this, LegalDocumentActivity.class);
        intent.putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, documentType);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
