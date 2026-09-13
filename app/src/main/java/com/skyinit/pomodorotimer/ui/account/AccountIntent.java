package com.skyinit.pomodorotimer.ui.account;

import android.net.Uri;

import androidx.annotation.Nullable;

/**
 * 个人资料页用户意图。
 */
public final class AccountIntent {

    public enum Type {
        OPEN_AVATAR,
        VIEW_AVATAR,
        CHANGE_AVATAR,
        PICK_FROM_GALLERY,
        TAKE_PHOTO,
        AVATAR_URI_SELECTED,
        OPEN_NICKNAME,
        OPEN_SIGNATURE,
        LOGOUT,
        CONFIRM_LOGOUT,
        UPGRADE_REGISTER,
        LOGIN,
        SWITCH_ACCOUNT,
        CONTINUE_AFTER_DISABLE_BLOCKING
    }

    public final Type type;
    @Nullable
    public final Uri uri;

    private AccountIntent(Type type, @Nullable Uri uri) {
        this.type = type;
        this.uri = uri;
    }

    public static AccountIntent openAvatar() {
        return new AccountIntent(Type.OPEN_AVATAR, null);
    }

    public static AccountIntent viewAvatar() {
        return new AccountIntent(Type.VIEW_AVATAR, null);
    }

    public static AccountIntent changeAvatar() {
        return new AccountIntent(Type.CHANGE_AVATAR, null);
    }

    public static AccountIntent pickFromGallery() {
        return new AccountIntent(Type.PICK_FROM_GALLERY, null);
    }

    public static AccountIntent takePhoto() {
        return new AccountIntent(Type.TAKE_PHOTO, null);
    }

    public static AccountIntent avatarUriSelected(Uri uri) {
        return new AccountIntent(Type.AVATAR_URI_SELECTED, uri);
    }

    public static AccountIntent openNickname() {
        return new AccountIntent(Type.OPEN_NICKNAME, null);
    }

    public static AccountIntent openSignature() {
        return new AccountIntent(Type.OPEN_SIGNATURE, null);
    }

    public static AccountIntent logout() {
        return new AccountIntent(Type.LOGOUT, null);
    }

    public static AccountIntent confirmLogout() {
        return new AccountIntent(Type.CONFIRM_LOGOUT, null);
    }

    public static AccountIntent upgradeRegister() {
        return new AccountIntent(Type.UPGRADE_REGISTER, null);
    }

    public static AccountIntent login() {
        return new AccountIntent(Type.LOGIN, null);
    }

    public static AccountIntent switchAccount() {
        return new AccountIntent(Type.SWITCH_ACCOUNT, null);
    }

    public static AccountIntent continueAfterDisablingBlocking() {
        return new AccountIntent(Type.CONTINUE_AFTER_DISABLE_BLOCKING, null);
    }
}
