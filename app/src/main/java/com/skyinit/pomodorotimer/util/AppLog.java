package com.skyinit.pomodorotimer.util;

import android.util.Log;

import com.skyinit.pomodorotimer.DebugConfig;

/**
 * 统一日志入口：调试日志仅在 Debug 构建输出，警告/错误在 Release 保留。
 */
public final class AppLog {

    private AppLog() {
    }

    public static void v(String tag, String message) {
        if (DebugConfig.isDebug()) {
            Log.v(tag, message);
        }
    }

    public static void d(String tag, String message) {
        if (DebugConfig.isDebug()) {
            Log.d(tag, message);
        }
    }

    public static void i(String tag, String message) {
        if (DebugConfig.isDebug()) {
            Log.i(tag, message);
        }
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
    }

    public static void w(String tag, String message, Throwable throwable) {
        Log.w(tag, message, throwable);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
}
