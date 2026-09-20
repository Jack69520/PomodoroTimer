package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import com.skyinit.pomodorotimer.TestApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link FocusDndStore} 持久化契约：拥有权字段须能跨「进程死亡」语义存活（同 prefs 再读）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class FocusDndStoreTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        FocusDndStore.clear(context);
    }

    @Test
    public void saveOwnership_persistsOwnedAndFilters() {
        FocusDndStore.saveOwnership(context, 1, 3);
        assertTrue(FocusDndStore.isOwned(context));
        assertEquals(1, FocusDndStore.getSavedFilter(context));
        assertEquals(3, FocusDndStore.getAppliedFilter(context));
    }

    @Test
    public void clear_removesOwnership() {
        FocusDndStore.saveOwnership(context, 2, 3);
        FocusDndStore.clear(context);
        assertFalse(FocusDndStore.isOwned(context));
        assertEquals(FocusDndStore.FILTER_UNSET, FocusDndStore.getSavedFilter(context));
        assertEquals(FocusDndStore.FILTER_UNSET, FocusDndStore.getAppliedFilter(context));
    }

    @Test
    public void getters_returnUnset_whenNotOwned() {
        assertFalse(FocusDndStore.isOwned(context));
        assertEquals(FocusDndStore.FILTER_UNSET, FocusDndStore.getSavedFilter(context));
        assertEquals(FocusDndStore.FILTER_UNSET, FocusDndStore.getAppliedFilter(context));
    }
}
