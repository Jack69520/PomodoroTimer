package com.skyinit.pomodorotimer.ui.profile;

/**
 * 「我的」页用户意图（MVI Intent）。
 */
public final class ProfileIntent {

    public enum Type {
        REFRESH,
        TOGGLE_BLOCKING,
        OPEN_ACCOUNT,
        OPEN_AVATAR_PREVIEW,
        OPEN_STATS,
        OPEN_MANAGE_BLOCKING,
        OPEN_SETTINGS,
        OPEN_FAQ,
        OPEN_ABOUT,
        OPEN_DEV_LAB,
        BLOCKING_EXTERNAL_RESULT
    }

    public final Type type;
    public final boolean blockingOn;
    public final boolean blockingSuccess;

    private ProfileIntent(Type type, boolean blockingOn, boolean blockingSuccess) {
        this.type = type;
        this.blockingOn = blockingOn;
        this.blockingSuccess = blockingSuccess;
    }

    public static ProfileIntent refresh() {
        return new ProfileIntent(Type.REFRESH, false, false);
    }

    public static ProfileIntent toggleBlocking(boolean on) {
        return new ProfileIntent(Type.TOGGLE_BLOCKING, on, false);
    }

    public static ProfileIntent openAccount() {
        return new ProfileIntent(Type.OPEN_ACCOUNT, false, false);
    }

    public static ProfileIntent openAvatarPreview() {
        return new ProfileIntent(Type.OPEN_AVATAR_PREVIEW, false, false);
    }

    public static ProfileIntent openStats() {
        return new ProfileIntent(Type.OPEN_STATS, false, false);
    }

    public static ProfileIntent openManageBlocking() {
        return new ProfileIntent(Type.OPEN_MANAGE_BLOCKING, false, false);
    }

    public static ProfileIntent openSettings() {
        return new ProfileIntent(Type.OPEN_SETTINGS, false, false);
    }

    public static ProfileIntent openFaq() {
        return new ProfileIntent(Type.OPEN_FAQ, false, false);
    }

    public static ProfileIntent openAbout() {
        return new ProfileIntent(Type.OPEN_ABOUT, false, false);
    }

    public static ProfileIntent openDevLab() {
        return new ProfileIntent(Type.OPEN_DEV_LAB, false, false);
    }

    public static ProfileIntent blockingExternalResult(boolean success) {
        return new ProfileIntent(Type.BLOCKING_EXTERNAL_RESULT, false, success);
    }
}
