package com.skyinit.pomodorotimer.ui;

import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;

/**
 * 子页基类：NoActionBar + Edge-to-Edge MaterialToolbar。
 */
public abstract class SubpageActivity extends BaseActivity {

    @Override
    protected void applyThemeStyle() {
        setTheme(R.style.Theme_PomodoroTimer_NoActionBar);
    }

    /** 设置布局并挂载 Edge-to-Edge 顶栏 / 内容 insets。 */
    protected void setContentWithSubpageChrome(int layoutResId, @StringRes int titleRes) {
        setContentView(layoutResId);
        setupSubpageEdgeToEdgeWithIme(titleRes);
    }

    @Override
    public boolean onPrepareOptionsMenu(@Nullable Menu menu) {
        tintSubpageMenuIcons(menu);
        return super.onPrepareOptionsMenu(menu);
    }
}
