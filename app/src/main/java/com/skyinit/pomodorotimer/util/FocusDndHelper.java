package com.skyinit.pomodorotimer.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.data.repository.FocusDndStore;

/**
 * 学习计时 <b>进行中</b> 临时启用系统勿扰（DND）；暂停或会话结束后恢复先前模式；休息不加勿扰。
 * <p>
 * <b>持久化：</b>开启前的 interruption filter 与拥有权写入 {@link FocusDndStore}，
 * 不以进程内静态字段为唯一真相源，从而在杀进程 / Alarm 唤醒后仍可恢复。
 * <p>
 * <b>恢复策略（产品约定，详见方法注释）：</b>
 * <ul>
 *   <li>暂停 / 会话结束路径 {@link #restoreDnd}：始终写回开始前保存的原状态（策略 2-A）。</li>
 *   <li>冷启动孤儿路径 {@link #recoverOrphanIfNeeded}：仅当系统当前仍为本 App
 *       所设模式时才恢复；用户已在系统设置中改过则只清盘（策略 3-A1）。
 *       若仍有活跃会话 checkpoint（含 paused），本路径跳过——paused 恢复须由
 *       {@code TimerService} 主动调用 {@link #restoreDnd}。</li>
 * </ul>
 * 全部入口经同一把类锁串行，保证多线程与「App 启动 + Service 同时进」的竞态安全。
 */
public final class FocusDndHelper {

    private static final String TAG = "FocusDndHelper";

    /** 类锁：保护 Store 读改写与系统 API 调用的顺序。 */
    private static final Object LOCK = new Object();

    private FocusDndHelper() {
    }

    public static boolean hasPolicyAccess(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    public static Intent createPolicyAccessIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        }
        return new Intent(Settings.ACTION_SETTINGS);
    }

    /**
     * 学习开始时尝试启用 DND；无权限或用户未开启偏好时静默跳过。
     * <p>
     * 顺序：采样原 filter → {@link FocusDndStore#saveOwnership}（commit）→
     * {@code setInterruptionFilter(NONE)}。若系统调用失败则回滚清盘。
     * <p>
     * 若磁盘已标记 owned（例如进程死后 checkpoint 恢复），不再重新采样当前 filter，
     * 避免把 NONE 误存为「原状态」导致结束时无法真正关闭勿扰。
     */
    public static void maybeEnableDnd(Context context, boolean enabledByUser) {
        if (!enabledByUser || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        Context app = context.getApplicationContext();
        synchronized (LOCK) {
            NotificationManager nm = app.getSystemService(NotificationManager.class);
            if (nm == null || !nm.isNotificationPolicyAccessGranted()) {
                AppLog.d(TAG, "DND policy access not granted, skipping enable");
                return;
            }
            int applied = NotificationManager.INTERRUPTION_FILTER_NONE;
            if (FocusDndStore.isOwned(app)) {
                // 已拥有：只确保系统仍为目标模式，不覆盖 saved_filter
                try {
                    if (nm.getCurrentInterruptionFilter() != applied) {
                        nm.setInterruptionFilter(applied);
                    }
                } catch (SecurityException e) {
                    AppLog.w(TAG, "Failed to re-apply DND while owned", e);
                }
                return;
            }
            int saved = nm.getCurrentInterruptionFilter();
            FocusDndStore.saveOwnership(app, saved, applied);
            try {
                nm.setInterruptionFilter(applied);
            } catch (SecurityException e) {
                AppLog.w(TAG, "Failed to enable DND, rolling back ownership", e);
                FocusDndStore.clear(app);
            }
        }
    }

    /**
     * 学习暂停 / 计时结束 / 失败 / 重置 / 切号时恢复先前勿扰模式（策略 2-A）。
     * <p>
     * <b>意图：</b>本会话内 App 视为临时接管勿扰，释放时始终写回开始前保存的原 filter，
     * 即使用户在专注期间手动改过系统勿扰也会被覆盖回原状态。
     * <p>
     * <b>日后若改为「尊重用户中途改动」：</b>可在写回前比较
     * {@code getCurrentInterruptionFilter()} 与 {@link FocusDndStore#getAppliedFilter}，
     * 仅当仍等于 applied 时才 set；否则只 clear。冷启动路径 {@link #recoverOrphanIfNeeded}
     * 已实现该条件逻辑，可作参考。
     * <p>
     * 无论 set 成败，finally 清盘，避免脏 owned 标记永久卡住。
     */
    public static void restoreDnd(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        Context app = context.getApplicationContext();
        synchronized (LOCK) {
            if (!FocusDndStore.isOwned(app)) {
                return;
            }
            int saved = FocusDndStore.getSavedFilter(app);
            NotificationManager nm = app.getSystemService(NotificationManager.class);
            try {
                if (nm != null && nm.isNotificationPolicyAccessGranted()
                        && saved != FocusDndStore.FILTER_UNSET) {
                    nm.setInterruptionFilter(saved);
                } else {
                    AppLog.d(TAG, "Cannot restore DND (no access or invalid saved filter); clearing ownership");
                }
            } catch (SecurityException e) {
                AppLog.w(TAG, "Failed to restore DND", e);
            } finally {
                FocusDndStore.clear(app);
            }
        }
    }

    /**
     * 冷启动 / Service 入口的孤儿勿扰兜底（策略 3-A1）。
     * <p>
     * <b>场景：</b>杀进程后结算异常、或旧版本仅内存保存导致 restore 失败，磁盘仍有 owned，
     * 系统可能仍停在本 App 设的 NONE。用户稍后冷启动 App 时应尽量修好。
     * <p>
     * <b>与 {@link #restoreDnd} 的差异：</b>用户可能在杀进程后于系统设置中自行改过勿扰；
     * 若无条件写回会覆盖用户手动操作。因此：
     * <ul>
     *   <li>存在活跃会话 checkpoint → 跳过（会话仍拥有，等正常结束走 2-A）。</li>
     *   <li>owned 且当前 filter == applied → 恢复 saved 并清盘。</li>
     *   <li>owned 但当前已变 → 仅清盘，不调用 setInterruptionFilter。</li>
     * </ul>
     * <p>
     * <b>边界：</b>若开启前原状态本身就是 NONE（saved == applied == NONE），无法区分
     * 「仍是 App 留下的」与「用户本就要 NONE」；条件恢复会再次 set 为 NONE 并清盘，可接受。
     * <p>
     * 方法幂等：可同时从 {@code App} 与 {@code TimerService} 调用。
     */
    public static void recoverOrphanIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        Context app = context.getApplicationContext();
        synchronized (LOCK) {
            if (ActiveSessionStore.hasActiveSession(app)) {
                AppLog.d(TAG, "Orphan DND recover skipped: active session owns DND");
                return;
            }
            if (!FocusDndStore.isOwned(app)) {
                return;
            }
            int applied = FocusDndStore.getAppliedFilter(app);
            int saved = FocusDndStore.getSavedFilter(app);
            NotificationManager nm = app.getSystemService(NotificationManager.class);
            if (nm == null || !nm.isNotificationPolicyAccessGranted()) {
                AppLog.d(TAG, "Orphan DND recover: no policy access, clearing ownership only");
                FocusDndStore.clear(app);
                return;
            }
            int current = nm.getCurrentInterruptionFilter();
            if (applied != FocusDndStore.FILTER_UNSET && current == applied) {
                // 系统仍为本 App 所设模式 → 视为遗留，执行恢复
                try {
                    if (saved != FocusDndStore.FILTER_UNSET) {
                        nm.setInterruptionFilter(saved);
                    }
                } catch (SecurityException e) {
                    AppLog.w(TAG, "Orphan DND restore failed", e);
                } finally {
                    FocusDndStore.clear(app);
                }
            } else {
                // 用户已在系统侧改过 → 尊重用户，只清标记
                AppLog.d(TAG, "Orphan DND recover: user changed filter (current=" + current
                        + ", applied=" + applied + "), clearing ownership only");
                FocusDndStore.clear(app);
            }
        }
    }
}
