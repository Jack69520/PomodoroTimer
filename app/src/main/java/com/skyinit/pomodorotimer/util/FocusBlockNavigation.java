package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.content.Intent;

import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.ui.home.TimerActivity;

/**
 * 应用屏蔽触发后的返回页：计时中回计时页，仅开启屏蔽时回「我的」页。
 */
public final class FocusBlockNavigation {

    public static final String EXTRA_NAV_DESTINATION = "extra_nav_destination";

    private FocusBlockNavigation() {
    }

    public static boolean shouldReturnToTimer(Context context) {
        return ActiveSessionStore.hasActiveSession(context);
    }

    public static Intent createReturnIntent(Context context) {
        if (shouldReturnToTimer(context)) {
            return new Intent(context, TimerActivity.class);
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_NAV_DESTINATION, R.id.nav_profile);
        return intent;
    }

    public static void openReturnDestination(Context context) {
        Intent intent = createReturnIntent(context);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
