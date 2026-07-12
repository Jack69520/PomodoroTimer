package com.skyinit.pomodorotimer.util;

import android.content.Intent;

/**
 * 静态 App Shortcuts 动作常量。
 */
public final class ShortcutActions {

    public static final String EXTRA_SHORTCUT_ACTION = "shortcut_action";

    /** 开始 25 分钟专注计时。 */
    public static final String ACTION_START_FOCUS_25 = "start_focus_25";

    /** 打开应用并跳转统计页。 */
    public static final String ACTION_VIEW_STATISTICS = "view_statistics";

    /** 打开我的页并尝试启用应用屏蔽。 */
    public static final String ACTION_ENABLE_BLOCKING = "enable_blocking";

    /** 将快捷方式动作从来源 Intent 复制到目标 Intent（若存在）。 */
    public static void copyShortcutAction(Intent source, Intent target) {
        if (source == null || target == null) {
            return;
        }
        String action = source.getStringExtra(EXTRA_SHORTCUT_ACTION);
        if (action != null) {
            target.putExtra(EXTRA_SHORTCUT_ACTION, action);
        }
    }

    private ShortcutActions() {
    }
}
