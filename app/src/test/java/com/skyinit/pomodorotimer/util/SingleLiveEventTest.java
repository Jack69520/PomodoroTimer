package com.skyinit.pomodorotimer.util;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SingleLiveEventTest {

    private final Owner owner = new Owner();

    @Before
    public void setUp() {
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Test
    public void postValue_setsPendingAndDelivers() {
        SingleLiveEvent<String> event = new SingleLiveEvent<>();
        AtomicInteger count = new AtomicInteger();
        event.observe(owner, v -> count.incrementAndGet());
        event.postValue("a");
        ShadowLooper.idleMainLooper();
        assertEquals(1, count.get());
    }

    @Test
    public void setValue_deliversOncePerEmit() {
        SingleLiveEvent<String> event = new SingleLiveEvent<>();
        AtomicInteger count = new AtomicInteger();
        event.observe(owner, v -> count.incrementAndGet());
        event.setValue("a");
        event.setValue("b");
        assertEquals(2, count.get());
    }

    private static final class Owner implements LifecycleOwner {
        final LifecycleRegistry registry = new LifecycleRegistry(this);

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }
}
