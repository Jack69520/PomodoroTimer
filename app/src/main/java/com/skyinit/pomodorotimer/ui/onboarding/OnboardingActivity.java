package com.skyinit.pomodorotimer.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.FirstRunRepository;
import com.skyinit.pomodorotimer.util.ShortcutActions;

import java.util.Arrays;
import java.util.List;

/**
 * 首次功能介绍页：ViewPager2 + 下一页 / 开始使用。
 */
public class OnboardingActivity extends BaseActivity {

    private ViewPager2 pager;
    private MaterialButton btnNext;
    private MaterialButton btnSkip;
    private LinearLayout indicator;
    private View[] dots;
    private int pageCount;

    @Override
    protected void applyThemeStyle() {
        setTheme(R.style.Theme_PomodoroTimer_NoActionBar);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirstRunRepository firstRun = FirstRunRepository.getInstance(this);
        if (firstRun.isOnboardingCompleted()) {
            goToMain();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_onboarding);

        pager = findViewById(R.id.onboarding_pager);
        btnNext = findViewById(R.id.btn_onboarding_next);
        btnSkip = findViewById(R.id.btn_onboarding_skip);
        indicator = findViewById(R.id.onboarding_indicator);

        List<OnboardingPagerAdapter.Page> pages = Arrays.asList(
                new OnboardingPagerAdapter.Page(
                        R.drawable.ic_timer,
                        R.string.onboarding_page1_title,
                        R.string.onboarding_page1_body),
                new OnboardingPagerAdapter.Page(
                        R.drawable.ic_check,
                        R.string.onboarding_page2_title,
                        R.string.onboarding_page2_body),
                new OnboardingPagerAdapter.Page(
                        R.drawable.ic_shortcut_blocking,
                        R.string.onboarding_page3_title,
                        R.string.onboarding_page3_body)
        );
        pageCount = pages.size();
        pager.setAdapter(new OnboardingPagerAdapter(pages));
        setupDots();
        updateChrome(0);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateChrome(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current < pageCount - 1) {
                pager.setCurrentItem(current + 1, true);
            } else {
                completeAndGoMain();
            }
        });
        btnSkip.setOnClickListener(v -> completeAndGoMain());

        applyWindowInsets();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pager.getCurrentItem() > 0) {
                    pager.setCurrentItem(pager.getCurrentItem() - 1, true);
                } else {
                    completeAndGoMain();
                }
            }
        });
    }

    private void setupDots() {
        dots = new View[pageCount];
        int size = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8f, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5f, getResources().getDisplayMetrics());
        indicator.removeAllViews();
        for (int i = 0; i < pageCount; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginStart(margin);
            lp.setMarginEnd(margin);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.bg_onboarding_dot);
            indicator.addView(dot);
            dots[i] = dot;
        }
    }

    private void updateChrome(int position) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setSelected(i == position);
        }
        boolean last = position == pageCount - 1;
        btnNext.setText(last ? R.string.onboarding_start : R.string.onboarding_next);
        btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
    }

    private void completeAndGoMain() {
        FirstRunRepository.getInstance(this).markOnboardingCompleted();
        goToMain();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        ShortcutActions.copyShortcutAction(getIntent(), intent);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.onboarding_root);
        if (root == null || btnSkip == null || btnNext == null || indicator == null || pager == null) {
            return;
        }
        final int skipPadL = btnSkip.getPaddingLeft();
        final int skipPadT = btnSkip.getPaddingTop();
        final int skipPadR = btnSkip.getPaddingRight();
        final int skipPadB = btnSkip.getPaddingBottom();
        final ViewGroup.MarginLayoutParams nextLp =
                (ViewGroup.MarginLayoutParams) btnNext.getLayoutParams();
        final int nextMarginBottom = nextLp.bottomMargin;
        final ViewGroup.MarginLayoutParams indicatorLp =
                (ViewGroup.MarginLayoutParams) indicator.getLayoutParams();
        final int indicatorMarginBottom = indicatorLp.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

            int top = Math.max(statusBars.top, cutout.top);
            int left = Math.max(navigationBars.left, cutout.left);
            int right = Math.max(navigationBars.right, cutout.right);
            int bottom = navigationBars.bottom;

            btnSkip.setPadding(skipPadL + left, skipPadT + top, skipPadR + right, skipPadB);
            pager.setPadding(left, 0, right, 0);
            nextLp.setMargins(nextLp.leftMargin, nextLp.topMargin, nextLp.rightMargin,
                    nextMarginBottom + bottom);
            btnNext.setLayoutParams(nextLp);
            indicatorLp.setMargins(indicatorLp.leftMargin, indicatorLp.topMargin,
                    indicatorLp.rightMargin, indicatorMarginBottom);
            indicator.setLayoutParams(indicatorLp);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
