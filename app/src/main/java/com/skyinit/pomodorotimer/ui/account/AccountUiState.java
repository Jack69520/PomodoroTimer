package com.skyinit.pomodorotimer.ui.account;

import androidx.annotation.Nullable;

/**
 * 个人资料页不可变 UI 快照。
 */
public final class AccountUiState {

    public final String userId;
    public final String idDisplay;
    public final String nickname;
    public final String signatureDisplay;
    public final boolean hasCustomSignature;
    @Nullable
    public final String avatarPath;
    public final boolean hasAvatar;
    public final boolean registered;
    public final boolean actionInProgress;

    public AccountUiState(String userId,
                          String idDisplay,
                          String nickname,
                          String signatureDisplay,
                          boolean hasCustomSignature,
                          @Nullable String avatarPath,
                          boolean hasAvatar,
                          boolean registered,
                          boolean actionInProgress) {
        this.userId = userId;
        this.idDisplay = idDisplay;
        this.nickname = nickname;
        this.signatureDisplay = signatureDisplay;
        this.hasCustomSignature = hasCustomSignature;
        this.avatarPath = avatarPath;
        this.hasAvatar = hasAvatar;
        this.registered = registered;
        this.actionInProgress = actionInProgress;
    }

    public AccountUiState withActionInProgress(boolean inProgress) {
        return new AccountUiState(
                userId, idDisplay, nickname, signatureDisplay, hasCustomSignature,
                avatarPath, hasAvatar, registered, inProgress);
    }
}
