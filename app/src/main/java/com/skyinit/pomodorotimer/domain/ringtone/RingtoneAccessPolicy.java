package com.skyinit.pomodorotimer.domain.ringtone;

import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.util.SettingsPermissionHelper;

/**
 * 提示音访问领域门面：哨兵值、音频读权限、软失效有效 URI、渠道 syncKey。
 * <p>
 * 无状态；不持 Activity、不写 prefs、不弹 UI。
 */
public final class RingtoneAccessPolicy {

    private RingtoneAccessPolicy() {
    }

    @NonNull
    public static SettingsPermissionHelper.Kind audioReadKind() {
        if (SettingsPermissionHelper.isApplicable(SettingsPermissionHelper.Kind.MUSIC_AUDIO)) {
            return SettingsPermissionHelper.Kind.MUSIC_AUDIO;
        }
        return SettingsPermissionHelper.Kind.MEDIA_FILES;
    }

    public static boolean hasAudioReadAccess(@NonNull Context context) {
        return SettingsPermissionHelper.isGranted(context, audioReadKind());
    }

    public static boolean needsAudioReadPermission(@NonNull Context context) {
        return !hasAudioReadAccess(context);
    }

    public static boolean isCustomPreference(@Nullable String preferenceRaw) {
        return resolveOption(preferenceRaw) == RingtoneAccessSnapshot.Option.CUSTOM;
    }

    @NonNull
    public static RingtoneAccessSnapshot.Option resolveOption(@Nullable String preferenceRaw) {
        if (preferenceRaw == null || preferenceRaw.isEmpty() || "default".equals(preferenceRaw)) {
            return RingtoneAccessSnapshot.Option.DEFAULT;
        }
        if ("silent".equals(preferenceRaw)) {
            return RingtoneAccessSnapshot.Option.SILENT;
        }
        Uri defaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (defaultNotification != null && defaultNotification.toString().equals(preferenceRaw)) {
            return RingtoneAccessSnapshot.Option.DEFAULT;
        }
        return RingtoneAccessSnapshot.Option.CUSTOM;
    }

    @NonNull
    public static RingtoneAccessSnapshot evaluate(@NonNull Context context,
                                                 @Nullable String preferenceRaw) {
        String raw = preferenceRaw == null || preferenceRaw.isEmpty() ? "default" : preferenceRaw;
        RingtoneAccessSnapshot.Option option = resolveOption(raw);
        boolean hasAccess = hasAudioReadAccess(context);
        Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        switch (option) {
            case SILENT:
                return new RingtoneAccessSnapshot(
                        raw, option, hasAccess, false, null, "silent");
            case DEFAULT:
                return new RingtoneAccessSnapshot(
                        raw, option, hasAccess, false, defaultUri, "default");
            case CUSTOM:
            default:
                if (hasAccess) {
                    Uri parsed = safeParse(raw);
                    if (parsed != null) {
                        return new RingtoneAccessSnapshot(
                                raw, option, true, false, parsed, raw + "|ok");
                    }
                    return new RingtoneAccessSnapshot(
                            raw, option, true, false, defaultUri, raw + "|invalid");
                }
                return new RingtoneAccessSnapshot(
                        raw, option, false, true, defaultUri, raw + "|fallback");
        }
    }

    public static boolean isCustomDegraded(@NonNull Context context, @Nullable String preferenceRaw) {
        return evaluate(context, preferenceRaw).degraded;
    }

    @Nullable
    public static Uri resolveEffectiveSoundUri(@NonNull Context context,
                                               @Nullable String preferenceRaw) {
        return evaluate(context, preferenceRaw).effectiveUri;
    }

    @NonNull
    public static String effectiveSyncKey(@NonNull Context context, @Nullable String preferenceRaw) {
        return evaluate(context, preferenceRaw).syncKey;
    }

    @Nullable
    private static Uri safeParse(@NonNull String raw) {
        try {
            Uri uri = Uri.parse(raw);
            if (uri == null || uri.getScheme() == null || uri.getScheme().isEmpty()) {
                return null;
            }
            return uri;
        } catch (Exception ignored) {
            return null;
        }
    }
}
