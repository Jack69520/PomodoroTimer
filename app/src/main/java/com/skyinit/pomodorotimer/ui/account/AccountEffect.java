package com.skyinit.pomodorotimer.ui.account;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * 个人资料页一次性副作用。
 */
public final class AccountEffect {

    public enum Type {
        SHOW_TOAST,
        SHOW_TOAST_TEXT,
        SHOW_AVATAR_ACTIONS,
        SHOW_PICK_AVATAR,
        SHOW_LOGOUT_CONFIRM,
        SHOW_GUARD_PROMPT,
        START_ACTIVITY,
        OPEN_IMAGE_PREVIEW,
        OPEN_GALLERY,
        OPEN_CAMERA
    }

    public final Type type;
    @StringRes
    public final int toastRes;
    @Nullable
    public final String toastText;
    @Nullable
    public final Class<? extends Activity> activityClass;
    @Nullable
    public final String imagePath;

    private AccountEffect(Type type,
                          @StringRes int toastRes,
                          @Nullable String toastText,
                          @Nullable Class<? extends Activity> activityClass,
                          @Nullable String imagePath) {
        this.type = type;
        this.toastRes = toastRes;
        this.toastText = toastText;
        this.activityClass = activityClass;
        this.imagePath = imagePath;
    }

    public static AccountEffect showToast(@StringRes int resId) {
        return new AccountEffect(Type.SHOW_TOAST, resId, null, null, null);
    }

    public static AccountEffect showToastText(String message) {
        return new AccountEffect(Type.SHOW_TOAST_TEXT, 0, message, null, null);
    }

    public static AccountEffect showAvatarActions() {
        return new AccountEffect(Type.SHOW_AVATAR_ACTIONS, 0, null, null, null);
    }

    public static AccountEffect showPickAvatar() {
        return new AccountEffect(Type.SHOW_PICK_AVATAR, 0, null, null, null);
    }

    public static AccountEffect showLogoutConfirm() {
        return new AccountEffect(Type.SHOW_LOGOUT_CONFIRM, 0, null, null, null);
    }

    public static AccountEffect showGuardPrompt() {
        return new AccountEffect(Type.SHOW_GUARD_PROMPT, 0, null, null, null);
    }

    public static AccountEffect startActivity(Class<? extends Activity> cls) {
        return new AccountEffect(Type.START_ACTIVITY, 0, null, cls, null);
    }

    public static AccountEffect openImagePreview(String path) {
        return new AccountEffect(Type.OPEN_IMAGE_PREVIEW, 0, null, null, path);
    }

    public static AccountEffect openGallery() {
        return new AccountEffect(Type.OPEN_GALLERY, 0, null, null, null);
    }

    public static AccountEffect openCamera() {
        return new AccountEffect(Type.OPEN_CAMERA, 0, null, null, null);
    }
}
