package com.skyinit.pomodorotimer.ui.consent;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;
import com.skyinit.pomodorotimer.ui.onboarding.FirstRunNavigator;
import com.skyinit.pomodorotimer.ui.profile.LegalDocumentActivity;

import android.content.Intent;
import android.graphics.Outline;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

/**
 * 首次启动隐私政策与用户协议同意页（应用入口）。
 */
public class PrivacyConsentActivity extends AppCompatActivity {

    private PrivacyConsentViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_PomodoroTimer_NoActionBar);
        super.onCreate(savedInstanceState);

        if (PrivacyConsentRepository.getInstance(this).hasAccepted()) {
            FirstRunNavigator.navigateAfterConsent(this, getIntent());
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_privacy_consent);
        applyWindowInsets();
        bindStaticContent();

        viewModel = new ViewModelProvider(
                this,
                new PrivacyConsentViewModelFactory(this)
        ).get(PrivacyConsentViewModel.class);

        viewModel.getAction().observe(this, action -> {
            if (action == null) {
                return;
            }
            switch (action) {
                case NAVIGATE_TO_MAIN:
                    ((App) getApplication()).initializeAfterConsent(
                            () -> FirstRunNavigator.navigateAfterConsent(this, getIntent()));
                    break;
                case EXIT_APP:
                    exitApp();
                    break;
                default:
                    break;
            }
        });

        MaterialButton btnAccept = findViewById(R.id.btn_consent_accept);
        MaterialButton btnDecline = findViewById(R.id.btn_consent_decline);
        btnAccept.setOnClickListener(v -> viewModel.onAcceptClicked());
        btnDecline.setOnClickListener(v -> viewModel.onDeclineClicked());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                viewModel.onDeclineClicked();
            }
        });
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.privacy_consent_root);
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void bindStaticContent() {
        ImageView icon = findViewById(R.id.iv_consent_app_icon);
        icon.setImageResource(R.mipmap.ic_launcher);
        applyRoundedSquareIconMask(icon);

        TextView linkPrivacy = findViewById(R.id.tv_consent_link_privacy);
        TextView linkAgreement = findViewById(R.id.tv_consent_link_agreement);
        setupLegalLink(linkPrivacy, getString(R.string.title_privacy_policy),
                LegalDocumentActivity.TYPE_PRIVACY_POLICY);
        setupLegalLink(linkAgreement, getString(R.string.title_user_agreement),
                LegalDocumentActivity.TYPE_USER_AGREEMENT);
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

    private void setupLegalLink(TextView textView, String label, String documentType) {
        SpannableString spannable = new SpannableString(label);
        int linkColor = ContextCompat.getColor(this, R.color.brand);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(PrivacyConsentActivity.this, LegalDocumentActivity.class);
                intent.putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, documentType);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(linkColor);
                ds.setUnderlineText(true);
            }
        }, 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(ContextCompat.getColor(this, android.R.color.transparent));
    }

    private void exitApp() {
        finishAffinity();
        System.exit(0);
    }
}
