package com.skyinit.pomodorotimer.ui.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.data.repository.FirstRunRepository;
import com.skyinit.pomodorotimer.util.ShortcutActions;

/**
 * 首次流程路由：同意后 → 首次注册 → 功能介绍 → 主页。
 */
public final class FirstRunNavigator {

    private FirstRunNavigator() {
    }

    @NonNull
    public static Class<?> nextDestination(@NonNull Context context) {
        FirstRunRepository repo = FirstRunRepository.getInstance(context);
        if (!repo.isAuthFlowCompleted()) {
            return FirstRegisterActivity.class;
        }
        if (!repo.isOnboardingCompleted()) {
            return OnboardingActivity.class;
        }
        return MainActivity.class;
    }

    public static void navigateAfterConsent(@NonNull Activity from, @NonNull Intent sourceIntent) {
        Intent intent = new Intent(from, nextDestination(from));
        ShortcutActions.copyShortcutAction(sourceIntent, intent);
        from.startActivity(intent);
        from.finish();
        from.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public static boolean redirectIfIncomplete(@NonNull Activity from, @NonNull Intent sourceIntent) {
        Class<?> next = nextDestination(from);
        if (next == MainActivity.class) {
            return false;
        }
        Intent intent = new Intent(from, next);
        ShortcutActions.copyShortcutAction(sourceIntent, intent);
        from.startActivity(intent);
        from.finish();
        from.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return true;
    }
}
