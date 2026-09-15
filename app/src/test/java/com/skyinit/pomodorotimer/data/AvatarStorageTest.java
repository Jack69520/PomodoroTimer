package com.skyinit.pomodorotimer.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.skyinit.pomodorotimer.TestApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class AvatarStorageTest {

    private Context context;
    private AvatarStorage storage;

    @Before
    public void setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        storage = AvatarStorage.getInstance();
    }

    @Test
    public void saveJpeg_writesFixedPath() {
        String userId = "123456789012";
        Bitmap bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.RED);

        String path = storage.saveJpeg(context, userId, bitmap);
        bitmap.recycle();

        assertNotNull(path);
        File expected = storage.getAvatarFile(context, userId);
        assertEquals(expected.getAbsolutePath(), path);
        assertTrue(expected.exists());
        assertTrue(expected.length() > 0);
    }

    @Test
    public void saveJpeg_twice_overwritesSameFile() {
        String userId = "223456789012";
        File avatarFile = storage.getAvatarFile(context, userId);
        File parent = avatarFile.getParentFile();
        assertNotNull(parent);

        Bitmap first = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        first.eraseColor(Color.BLUE);
        assertNotNull(storage.saveJpeg(context, userId, first));
        first.recycle();

        String[] before = parent.list((dir, name) -> name.startsWith("avatar_" + userId));
        assertNotNull(before);
        assertEquals(1, before.length);

        Bitmap second = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        second.eraseColor(Color.GREEN);
        assertNotNull(storage.saveJpeg(context, userId, second));
        second.recycle();

        String[] after = parent.list((dir, name) -> name.startsWith("avatar_" + userId));
        assertNotNull(after);
        assertEquals(1, after.length);
        assertEquals("avatar_" + userId + ".jpg", after[0]);
        assertTrue(avatarFile.exists());
    }

    @Test
    public void deleteForUser_removesAvatarFile() {
        String userId = "323456789012";
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        assertNotNull(storage.saveJpeg(context, userId, bitmap));
        bitmap.recycle();

        File avatarFile = storage.getAvatarFile(context, userId);
        assertTrue(avatarFile.exists());

        storage.deleteForUser(context, userId);
        assertFalse(avatarFile.exists());
    }

    @Test
    public void cameraCaptureFile_createAndDelete() {
        File capture = storage.createCameraCaptureFile(context);
        assertNotNull(capture.getParentFile());
        assertEquals("avatar_capture.jpg", capture.getName());

        try {
            assertTrue(capture.createNewFile() || capture.exists());
        } catch (Exception e) {
            throw new AssertionError("Failed to create capture file", e);
        }
        assertTrue(capture.exists());

        storage.deleteCameraCaptureFile(context);
        assertFalse(capture.exists());
    }
}
