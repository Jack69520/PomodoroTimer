package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PrivacyConsentRepositoryTest {

    private Context context;

    @Before
    public void setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        context.getSharedPreferences("privacy_consent_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void accept_marksConsentGiven() {
        PrivacyConsentRepository repository = PrivacyConsentRepository.getInstance(context);
        assertFalse(repository.hasAccepted());
        repository.accept();
        assertTrue(repository.hasAccepted());
    }
}
