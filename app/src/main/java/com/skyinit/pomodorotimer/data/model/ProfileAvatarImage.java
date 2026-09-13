package com.skyinit.pomodorotimer.data.model;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

/**
 * 头像异步加载结果；path 用于与当前账户对齐，bitmap 为空表示使用默认头像。
 */
public final class ProfileAvatarImage {

    @Nullable
    public final String path;
    @Nullable
    public final Bitmap bitmap;

    public ProfileAvatarImage(@Nullable String path, @Nullable Bitmap bitmap) {
        this.path = path;
        this.bitmap = bitmap;
    }

    public static ProfileAvatarImage none() {
        return new ProfileAvatarImage(null, null);
    }
}
