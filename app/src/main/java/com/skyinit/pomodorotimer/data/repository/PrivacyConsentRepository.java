package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 首次启动隐私政策与用户协议同意状态（本地持久化，不上传）。
 */
public final class PrivacyConsentRepository {

    private static final String PREFS_NAME = "privacy_consent_prefs";
    private static final String KEY_ACCEPTED = "privacy_consent_accepted";
    /** 与 assets 中隐私政策版本号一致，政策重大变更时可递增以要求重新同意。 */
    private static final String KEY_POLICY_VERSION = "privacy_consent_policy_version";
    public static final int CURRENT_POLICY_VERSION = 1;

    private static volatile PrivacyConsentRepository instance;

    private final SharedPreferences prefs;

    private PrivacyConsentRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static PrivacyConsentRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (PrivacyConsentRepository.class) {
                if (instance == null) {
                    instance = new PrivacyConsentRepository(context);
                }
            }
        }
        return instance;
    }

    public boolean hasAccepted() {
        return prefs.getBoolean(KEY_ACCEPTED, false)
                && prefs.getInt(KEY_POLICY_VERSION, 0) == CURRENT_POLICY_VERSION;
    }

    public void accept() {
        prefs.edit()
                .putBoolean(KEY_ACCEPTED, true)
                .putInt(KEY_POLICY_VERSION, CURRENT_POLICY_VERSION)
                .commit();
    }
}
