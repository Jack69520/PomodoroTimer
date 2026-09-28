package com.skyinit.pomodorotimer.ui.account;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;

/**
 * 强制改密硬闸门：当前会话用户 {@code mustResetPassword} 时，禁止进入主功能，
 * 仅允许停留在 {@link SetNewPasswordActivity} 或显式登出。
 */
public final class ForcePasswordResetNavigator {

    private ForcePasswordResetNavigator() {
    }

    /**
     * @return true 表示已跳转并应中止当前页后续初始化
     */
    public static boolean redirectIfRequired(@NonNull Activity from) {
        if (from instanceof SetNewPasswordActivity) {
            return false;
        }
        App app = (App) from.getApplication();
        if (!app.isUserDataInitialized()) {
            return false;
        }
        UserSessionRepository session = app.getContainer().getUserSessionRepository();
        if (!session.isForcePasswordReset()) {
            return false;
        }
        Intent intent = new Intent(from, SetNewPasswordActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        from.startActivity(intent);
        from.finish();
        from.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return true;
    }
}
