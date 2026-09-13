package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class AboutActivity extends SubpageActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_about, R.string.title_about);

        try {
            TextView appName = findViewById(R.id.tv_app_name);
            appName.setText(getString(R.string.app_name));

            ImageView appIcon = findViewById(R.id.iv_app_icon);
            appIcon.setImageResource(R.mipmap.ic_launcher);
            applyRoundedSquareIconMask(appIcon);

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

    /**
     * 主动对应用图标做圆角方形蒙版裁剪（约等于桌面自适应图标外观）。
     */
    private void applyRoundedSquareIconMask(@NonNull ImageView icon) {
        icon.setClipToOutline(true);
        icon.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = Math.min(view.getWidth(), view.getHeight());
                // 圆角半径约为边长 22%，贴近系统圆角方形图标蒙版
                float radius = size * 0.22f;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
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
