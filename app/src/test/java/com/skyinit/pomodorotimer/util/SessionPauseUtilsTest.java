package com.skyinit.pomodorotimer.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SessionPauseUtilsTest {

    @Test
    public void encodeDecode_preservesEmptySlots() {
        List<String> original = Arrays.asList("", "被打断", "");
        String encoded = SessionPauseUtils.encodeReasons(original);
        List<String> decoded = SessionPauseUtils.decodeReasons(encoded, null);
        assertEquals(3, decoded.size());
        assertEquals("", decoded.get(0));
        assertEquals("被打断", decoded.get(1));
        assertEquals("", decoded.get(2));
    }

    @Test
    public void encode_nonEmptyRoundTrip() {
        List<String> original = Arrays.asList("临时有事", "快速恢复");
        String encoded = SessionPauseUtils.encodeReasons(original);
        assertTrue(encoded.contains("临时有事"));
        assertEquals(original, SessionPauseUtils.decodeReasons(encoded, null));
    }
}
