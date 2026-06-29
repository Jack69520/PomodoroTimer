package com.skyinit.pomodorotimer;

import androidx.work.testing.WorkManagerTestInitHelper;

import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;

/**
 * Robolectric 测试用 Application：在 {@link App#onCreate()} 前初始化 WorkManager。
 */
public class TestApp extends App {

    @Override
    public void onCreate() {
        WorkManagerTestInitHelper.initializeTestWorkManager(this);
        PrivacyConsentRepository.getInstance(this).accept();
        super.onCreate();
    }
}
