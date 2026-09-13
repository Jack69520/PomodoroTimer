package com.skyinit.pomodorotimer.ui.profile;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * 「我的」页一次性副作用（Toast / 导航 / 权限引导）。
 */
public final class ProfileEffect {

    public enum Type {
        SHOW_TOAST,
        START_ACTIVITY,
        NAVIGATE_TO_STATISTICS,
        REQUEST_ENABLE_BLOCKING,
        OPEN_IMAGE_PREVIEW,
        SHOW_DEV_LAB_CONFIRM,
        REQUIRE_AUTH
    }

    public final Type type;
    @StringRes
    public final int toastRes;
    public final boolean toastLong;
    @Nullable
    public final Class<? extends Activity> activityClass;
    @Nullable
    public final String imagePath;

    private ProfileEffect(Type type,
                          @StringRes int toastRes,
                          boolean toastLong,
                          @Nullable Class<? extends Activity> activityClass,
                          @Nullable String imagePath) {
        this.type = type;
        this.toastRes = toastRes;
        this.toastLong = toastLong;
        this.activityClass = activityClass;
        this.imagePath = imagePath;
    }

    public static ProfileEffect showToast(@StringRes int resId, boolean longDuration) {
        return new ProfileEffect(Type.SHOW_TOAST, resId, longDuration, null, null);
    }

    public static ProfileEffect startActivity(Class<? extends Activity> activityClass) {
        return new ProfileEffect(Type.START_ACTIVITY, 0, false, activityClass, null);
    }

    public static ProfileEffect navigateToStatistics() {
        return new ProfileEffect(Type.NAVIGATE_TO_STATISTICS, 0, false, null, null);
    }

    public static ProfileEffect requestEnableBlocking() {
        return new ProfileEffect(Type.REQUEST_ENABLE_BLOCKING, 0, false, null, null);
    }

    public static ProfileEffect openImagePreview(String path) {
        return new ProfileEffect(Type.OPEN_IMAGE_PREVIEW, 0, false, null, path);
    }

    public static ProfileEffect showDevLabConfirm() {
        return new ProfileEffect(Type.SHOW_DEV_LAB_CONFIRM, 0, false, null, null);
    }

    public static ProfileEffect requireAuth() {
        return new ProfileEffect(Type.REQUIRE_AUTH, 0, false, null, null);
    }
}
