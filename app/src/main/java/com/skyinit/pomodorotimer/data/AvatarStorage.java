package com.skyinit.pomodorotimer.data;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.util.AppLog;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 账号头像与拍照捕获临时文件的磁盘生命周期。
 * 约定：正式头像固定为 {@code avatars/avatar_{userId}.jpg}（覆盖写）。
 */
public final class AvatarStorage {

    private static final String TAG = "AvatarStorage";
    private static final String AVATARS_DIR = "avatars";
    private static final String CAPTURE_FILE_NAME = "avatar_capture.jpg";
    private static final int JPEG_QUALITY = 85;

    private static final AvatarStorage INSTANCE = new AvatarStorage();

    private AvatarStorage() {
    }

    public static AvatarStorage getInstance() {
        return INSTANCE;
    }

    @NonNull
    public File getAvatarFile(@NonNull Context context, @NonNull String userId) {
        File dir = new File(context.getApplicationContext().getExternalFilesDir(null), AVATARS_DIR);
        return new File(dir, "avatar_" + userId + ".jpg");
    }

    /**
     * 覆盖写入 JPEG 头像。成功返回绝对路径；失败返回 null。
     */
    @Nullable
    public String saveJpeg(@NonNull Context context, @NonNull String userId, @NonNull Bitmap bitmap) {
        File avatarFile = getAvatarFile(context, userId);
        File parent = avatarFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            AppLog.w(TAG, "Failed to create avatars directory");
            return null;
        }
        try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
            boolean ok = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
            if (!ok) {
                AppLog.w(TAG, "JPEG compress returned false");
                return null;
            }
            return avatarFile.getAbsolutePath();
        } catch (Exception e) {
            AppLog.w(TAG, "Save avatar failed", e);
            return null;
        }
    }

    /** Best-effort：删除该用户固定头像文件。 */
    public void deleteForUser(@NonNull Context context, @NonNull String userId) {
        File avatarFile = getAvatarFile(context, userId);
        try {
            if (avatarFile.exists() && !avatarFile.delete()) {
                AppLog.w(TAG, "Failed to delete avatar: " + avatarFile.getAbsolutePath());
            }
        } catch (Exception e) {
            AppLog.w(TAG, "Delete avatar failed", e);
        }
    }

    @NonNull
    public File createCameraCaptureFile(@NonNull Context context) {
        Context app = context.getApplicationContext();
        File cacheDir = app.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = app.getCacheDir();
        }
        if (cacheDir != null && !cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        }
        return new File(cacheDir, CAPTURE_FILE_NAME);
    }

    public void deleteCameraCaptureFile(@NonNull Context context) {
        try {
            File capture = createCameraCaptureFile(context);
            if (capture.exists() && !capture.delete()) {
                AppLog.w(TAG, "Failed to delete capture file: " + capture.getAbsolutePath());
            }
        } catch (Exception e) {
            AppLog.w(TAG, "Delete capture file failed", e);
        }
    }
}
