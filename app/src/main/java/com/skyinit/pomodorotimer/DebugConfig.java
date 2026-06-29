package com.skyinit.pomodorotimer;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * 运行时 Debug 标志，替代 Gradle 生成的 {@code BuildConfig}，避免 IDE 无法索引生成类而标红。
 * 须在 {@link App#onCreate()} 中调用 {@link #init(Context)}。
 */
public final class DebugConfig {

    private static volatile boolean initialized;
    private static boolean debug;

    private DebugConfig() {
    }

    public static void init(Context context) {
        if (initialized) {
            return;
        }
        synchronized (DebugConfig.class) {
            if (initialized) {
                return;
            }
            ApplicationInfo info = context.getApplicationContext().getApplicationInfo();
            debug = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            initialized = true;
        }
    }

    public static boolean isDebug() {
        return debug;
    }
}
