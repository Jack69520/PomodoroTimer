package com.skyinit.pomodorotimer.domain.ringtone;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 提示音访问快照：偏好原值与「有效可播 URI」分离（R1 软失效）。
 */
public final class RingtoneAccessSnapshot {

    public enum Option {
        DEFAULT,
        SILENT,
        CUSTOM
    }

    @NonNull
    public final String preferenceRaw;
    @NonNull
    public final Option option;
    public final boolean hasAudioReadAccess;
    /** 自定义偏好但缺少读音频权限。 */
    public final boolean degraded;
    /** 实际用于播放/通知渠道；静音为 null。 */
    @Nullable
    public final Uri effectiveUri;
    /**
     * 渠道同步缓存键：权限翻转时必须变化，避免跳过重同步。
     */
    @NonNull
    public final String syncKey;

    public RingtoneAccessSnapshot(@NonNull String preferenceRaw,
                                  @NonNull Option option,
                                  boolean hasAudioReadAccess,
                                  boolean degraded,
                                  @Nullable Uri effectiveUri,
                                  @NonNull String syncKey) {
        this.preferenceRaw = preferenceRaw;
        this.option = option;
        this.hasAudioReadAccess = hasAudioReadAccess;
        this.degraded = degraded;
        this.effectiveUri = effectiveUri;
        this.syncKey = syncKey;
    }
}
