package com.skyinit.pomodorotimer.util;

/**
 * 静态 App Shortcuts 动作常量。
 */
public final class ShortcutActions {

    public static final String EXTRA_SHORTCUT_ACTION = "shortcut_action";

    /** 开始 25 分钟专注计时。 */
    public static final String ACTION_START_FOCUS_25 = "start_focus_25";

    /** 打开应用并跳转统计页。 */
    public static final String ACTION_VIEW_STATISTICS = "view_statistics";

    /** 打开设置页并尝试启用应用屏蔽。 */
    public static final String ACTION_ENABLE_BLOCKING = "enable_blocking";

    private ShortcutActions() {
    }
}
