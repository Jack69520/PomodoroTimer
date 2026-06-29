package com.skyinit.pomodorotimer.data.model;

/**
 * 个人页账户卡片 UI 状态。
 */
public final class ProfileUiState {

    public final String nickname;
    public final String idLabel;
    public final String avatarPath;
    public final boolean hasAvatar;

    public ProfileUiState(String nickname, String idLabel, String avatarPath, boolean hasAvatar) {
        this.nickname = nickname;
        this.idLabel = idLabel;
        this.avatarPath = avatarPath;
        this.hasAvatar = hasAvatar;
    }
}
