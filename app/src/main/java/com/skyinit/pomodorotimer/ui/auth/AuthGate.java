package com.skyinit.pomodorotimer.ui.auth;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.ui.account.LoginActivity;
import com.skyinit.pomodorotimer.ui.account.RegisterActivity;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;

/**
 * 统一注册/登录引导弹窗。
 */
public final class AuthGate {

    private AuthGate() {
    }

    public static void show(@NonNull Activity activity) {
        show(activity, R.string.auth_gate_message);
    }

    public static void show(@NonNull Activity activity, int messageRes) {
        if (activity.isFinishing()) {
            return;
        }
        ModernPromptDialog.builder(activity)
                .icon(R.drawable.ic_lock)
                .accent(ModernPromptDialog.Accent.BRAND)
                .title(R.string.auth_gate_title)
                .message(messageRes)
                .primary(R.string.auth_gate_register, () ->
                        activity.startActivity(new Intent(activity, RegisterActivity.class)))
                .secondary(R.string.auth_gate_login, () ->
                        activity.startActivity(new Intent(activity, LoginActivity.class)))
                .tertiary(R.string.cancel, null)
                .show();
    }

    public static boolean ensureLoggedIn(@NonNull FragmentActivity activity,
                                         boolean loggedIn) {
        if (loggedIn) {
            return true;
        }
        show(activity);
        return false;
    }
}
