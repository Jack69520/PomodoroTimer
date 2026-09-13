package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.SubpageActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class LegalDocumentActivity extends SubpageActivity {

    public static final String EXTRA_DOCUMENT_TYPE = "document_type";
    public static final String TYPE_PRIVACY_POLICY = "privacy_policy";
    public static final String TYPE_USER_AGREEMENT = "user_agreement";

    private static final String ASSET_PRIVACY_POLICY = "legal/privacy_policy.html";
    private static final String ASSET_USER_AGREEMENT = "legal/user_agreement.html";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String documentType = getIntent().getStringExtra(EXTRA_DOCUMENT_TYPE);
        if (documentType == null) {
            finish();
            return;
        }

        int titleRes;
        String assetPath;
        if (TYPE_USER_AGREEMENT.equals(documentType)) {
            titleRes = R.string.title_user_agreement;
            assetPath = ASSET_USER_AGREEMENT;
        } else {
            titleRes = R.string.title_privacy_policy;
            assetPath = ASSET_PRIVACY_POLICY;
        }

        if (!assetExists(assetPath)) {
            Toast.makeText(this, R.string.legal_document_load_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentWithSubpageChrome(R.layout.activity_legal_document, titleRes);
        setupWebView(findViewById(R.id.subpage_content), assetPath);
    }

    private void setupWebView(WebView webView, String assetPath) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setBackgroundColor(0);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 仅展示本地法律文档，拦截一切外链跳转
                return true;
            }
        });
        webView.loadUrl("file:///android_asset/" + assetPath);
    }

    private boolean assetExists(String assetPath) {
        try (InputStream ignored = getAssets().open(assetPath)) {
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
