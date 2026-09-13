package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * 设备级首次流程标记：首次注册引导、功能介绍。
 */
public final class FirstRunRepository {

    private static final String PREFS = "first_run_prefs";
    private static final String KEY_AUTH_FLOW_DONE = "first_auth_flow_completed";
    private static final String KEY_ONBOARDING_DONE = "onboarding_completed";

    private static FirstRunRepository instance;
    private final SharedPreferences prefs;

    private FirstRunRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized FirstRunRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new FirstRunRepository(context);
        }
        return instance;
    }

    public boolean isAuthFlowCompleted() {
        return prefs.getBoolean(KEY_AUTH_FLOW_DONE, false);
    }

    public void markAuthFlowCompleted() {
        prefs.edit().putBoolean(KEY_AUTH_FLOW_DONE, true).commit();
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void markOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).commit();
    }
}
