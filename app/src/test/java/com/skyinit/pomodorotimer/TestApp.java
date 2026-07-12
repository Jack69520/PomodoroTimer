package com.skyinit.pomodorotimer;

import androidx.work.testing.WorkManagerTestInitHelper;

import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;

/**
 * Robolectric 测试用 Application：每次 onCreate 同步完成干净的用户数据引导。
 */
public class TestApp extends App {

    @Override
    protected boolean shouldAutoInitializeAfterConsent() {
        return false;
    }

    @Override
    public void onCreate() {
        WorkManagerTestInitHelper.initializeTestWorkManager(this);
        PrivacyConsentRepository.getInstance(this).accept();
        super.onCreate();
        resetTestState();
        initializeAfterConsentSyncForTest();
    }

    static void resetTestState() {
        App.resetUserDataStateForTest();
        AppDatabase.resetForTest();
        AccountManager.resetForTest();
        AppContainer.resetForTest();
    }
}
