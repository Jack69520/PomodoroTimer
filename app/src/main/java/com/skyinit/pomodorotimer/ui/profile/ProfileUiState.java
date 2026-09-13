package com.skyinit.pomodorotimer.ui.profile;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * 「我的」页不可变 UI 快照。
 */
public final class ProfileUiState {

    public final String nickname;
    public final String idLabel;
    public final String signatureText;
    public final boolean hasSignature;
    @Nullable
    public final String avatarPath;
    public final boolean hasAvatar;
    public final boolean accountSectionEnabled;

    public final int totalCompletedCount;
    public final long totalFocusDurationMs;
    /** 当前档案统计是否已返回；未就绪时 UI 不展示闪零。 */
    public final boolean statsReady;
    public final boolean statsClickable;

    public final boolean blockingChecked;
    @StringRes
    public final int blockingDescriptionRes;
    public final boolean blockingToggleEnabled;
    public final boolean blockingBusy;
    public final boolean manageBlockingEnabled;

    public final boolean devLabVisible;

    public ProfileUiState(String nickname,
                          String idLabel,
                          String signatureText,
                          boolean hasSignature,
                          @Nullable String avatarPath,
                          boolean hasAvatar,
                          boolean accountSectionEnabled,
                          int totalCompletedCount,
                          long totalFocusDurationMs,
                          boolean statsReady,
                          boolean statsClickable,
                          boolean blockingChecked,
                          @StringRes int blockingDescriptionRes,
                          boolean blockingToggleEnabled,
                          boolean blockingBusy,
                          boolean manageBlockingEnabled,
                          boolean devLabVisible) {
        this.nickname = nickname;
        this.idLabel = idLabel;
        this.signatureText = signatureText;
        this.hasSignature = hasSignature;
        this.avatarPath = avatarPath;
        this.hasAvatar = hasAvatar;
        this.accountSectionEnabled = accountSectionEnabled;
        this.totalCompletedCount = totalCompletedCount;
        this.totalFocusDurationMs = totalFocusDurationMs;
        this.statsReady = statsReady;
        this.statsClickable = statsClickable;
        this.blockingChecked = blockingChecked;
        this.blockingDescriptionRes = blockingDescriptionRes;
        this.blockingToggleEnabled = blockingToggleEnabled;
        this.blockingBusy = blockingBusy;
        this.manageBlockingEnabled = manageBlockingEnabled;
        this.devLabVisible = devLabVisible;
    }
}
