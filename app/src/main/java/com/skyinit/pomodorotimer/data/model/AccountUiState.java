package com.skyinit.pomodorotimer.data.model;

/**
 * 账户详情页 UI 状态。
 */
public final class AccountUiState {

    public final String userId;
    public final String idLabel;
    public final String nickname;
    public final String signature;
    public final String avatarPath;
    public final boolean registered;
    public final boolean actionInProgress;

    public AccountUiState(String userId,
                          String idLabel,
                          String nickname,
                          String signature,
                          String avatarPath,
                          boolean registered,
                          boolean actionInProgress) {
        this.userId = userId;
        this.idLabel = idLabel;
        this.nickname = nickname;
        this.signature = signature;
        this.avatarPath = avatarPath;
        this.registered = registered;
        this.actionInProgress = actionInProgress;
    }

    public AccountUiState withActionInProgress(boolean inProgress) {
        return new AccountUiState(userId, idLabel, nickname, signature, avatarPath, registered, inProgress);
    }
}
