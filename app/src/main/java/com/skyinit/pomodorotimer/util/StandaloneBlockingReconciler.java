package com.skyinit.pomodorotimer.util;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.AppInitializationRepository;
import com.skyinit.pomodorotimer.data.repository.UserAppBlockingRepository;
import com.skyinit.pomodorotimer.domain.blocking.BlockingRuntimeState;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 独立屏蔽 B2 协调：orphan 时询问是否重新开启，绝不静默 startStandalone。
 */
public final class StandaloneBlockingReconciler {

    private static final String TAG = "StandaloneBlockingReconciler";
    private static final StandaloneBlockingReconciler INSTANCE = new StandaloneBlockingReconciler();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger generation = new AtomicInteger(0);
    private final AtomicLong orphanEpoch = new AtomicLong(0L);
    private final AtomicLong answeredEpoch = new AtomicLong(-1L);
    private final AtomicBoolean dialogShowing = new AtomicBoolean(false);
    private final AtomicBoolean warmInFlight = new AtomicBoolean(false);

    @Nullable
    private AlertDialog activeDialog;

    private StandaloneBlockingReconciler() {
    }

    public static StandaloneBlockingReconciler getInstance() {
        return INSTANCE;
    }

    /** 切号 / 登出时复位，使下一 orphan 可再询问。 */
    public void resetSessionFlags() {
        generation.incrementAndGet();
        answeredEpoch.set(-1L);
        orphanEpoch.set(0L);
        dismissDialogQuietly();
        dialogShowing.set(false);
        warmInFlight.set(false);
    }

    /**
     * 用户主动启停独立屏蔽时调用：使未完成对话框回调失效，
     * 并重新武装 orphan 检测（下次进程内停服仍可再问）。
     */
    public void invalidatePendingPrompt() {
        generation.incrementAndGet();
        dismissDialogQuietly();
        dialogShowing.set(false);
        orphanEpoch.set(0L);
        answeredEpoch.set(-1L);
    }

    @MainThread
    public void maybeReconcile(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) {
            return;
        }
        Application app = activity.getApplication();
        if (!(app instanceof App)) {
            return;
        }
        if (((App) app).getInitializationRepository().getCurrentState()
                != AppInitializationRepository.State.READY) {
            return;
        }
        if (!AccountManager.getInstance(activity).hasActiveSession()) {
            return;
        }

        UserAppBlockingRepository repo = AppContainer.getInstance(activity)
                .getUserAppBlockingRepository();
        if (!repo.isCacheWarmForCurrentUser()) {
            scheduleWarmThenReconcile(activity, repo);
            return;
        }
        reconcileWithWarmCache(activity, repo);
    }

    private void scheduleWarmThenReconcile(@NonNull Activity activity,
                                           @NonNull UserAppBlockingRepository repo) {
        if (!warmInFlight.compareAndSet(false, true)) {
            return;
        }
        final int gen = generation.get();
        AppExecutors.getInstance().diskIo(() -> {
            try {
                String userId = AccountManager.getInstance(activity).getCurrentUserId();
                repo.warmForUser(userId);
            } catch (Throwable t) {
                AppLog.e(TAG, "Failed to warm blocking prefs", t);
            } finally {
                mainHandler.post(() -> {
                    warmInFlight.set(false);
                    if (gen != generation.get()) {
                        return;
                    }
                    maybeReconcile(activity);
                });
            }
        });
    }

    @MainThread
    private void reconcileWithWarmCache(@NonNull Activity activity,
                                        @NonNull UserAppBlockingRepository repo) {
        boolean prefsOn = repo.isEnabledForCurrentUser();
        boolean perms = PermissionUtils.hasAllAppBlockingPermissions(activity);
        boolean standaloneActive = BlockingRuntimeState.isStandaloneActive();

        if (!prefsOn || !perms) {
            return;
        }
        if (standaloneActive) {
            // 运行中：清除 epoch，便于之后再次变为 orphan 时重新询问
            orphanEpoch.set(0L);
            answeredEpoch.set(-1L);
            return;
        }

        // orphan
        long epoch = orphanEpoch.get();
        if (epoch == 0L) {
            epoch = System.currentTimeMillis();
            orphanEpoch.set(epoch);
        }
        if (answeredEpoch.get() == epoch) {
            return;
        }
        if (!dialogShowing.compareAndSet(false, true)) {
            return;
        }

        final int gen = generation.incrementAndGet();
        final long epochFinal = epoch;
        try {
            AlertDialog dialog = ModernPromptDialog.builder(activity)
                    .accent(ModernPromptDialog.Accent.BRAND)
                    .title(R.string.blocking_orphan_dialog_title)
                    .message(R.string.blocking_orphan_dialog_message)
                    .primary(R.string.blocking_orphan_dialog_reenable, () ->
                            onAnswer(activity, gen, epochFinal, true))
                    .secondary(R.string.blocking_orphan_dialog_disable, () ->
                            onAnswer(activity, gen, epochFinal, false))
                    .cancelable(true)
                    .show();
            activeDialog = dialog;
            dialog.setOnCancelListener(d -> onAnswer(activity, gen, epochFinal, false));
            dialog.setOnDismissListener(d -> {
                if (activeDialog == dialog) {
                    activeDialog = null;
                }
                dialogShowing.set(false);
            });
        } catch (Exception e) {
            dialogShowing.set(false);
            AppLog.e(TAG, "Failed to show orphan dialog", e);
        }
    }

    @MainThread
    private void onAnswer(@NonNull Activity activity, int gen, long epoch, boolean reenable) {
        if (gen != generation.get()) {
            return;
        }
        answeredEpoch.set(epoch);
        dismissDialogQuietly();
        dialogShowing.set(false);

        UserAppBlockingRepository repo = AppContainer.getInstance(activity)
                .getUserAppBlockingRepository();
        if (reenable) {
            if (!PermissionUtils.hasAllAppBlockingPermissions(activity)
                    || !AccountManager.getInstance(activity).hasActiveSession()) {
                Toast.makeText(activity, R.string.blocking_toast_start_failed, Toast.LENGTH_LONG).show();
                return;
            }
            repo.setEnabledForCurrentUser(true);
            AppBlockingServiceUtils.startStandaloneBlocking(activity);
            // 重新武装：服务若再次在同进程内停止，仍可询问
            orphanEpoch.set(0L);
            answeredEpoch.set(-1L);
            mainHandler.postDelayed(() -> {
                if (gen != generation.get()) {
                    return;
                }
                if (!BlockingRuntimeState.isStandaloneActive()
                        && repo.isEnabledForCurrentUser()) {
                    Toast.makeText(activity, R.string.blocking_toast_start_failed,
                            Toast.LENGTH_LONG).show();
                }
            }, 800L);
        } else {
            repo.setEnabledForCurrentUser(false);
            AppBlockingServiceUtils.stopStandaloneBlocking(activity);
        }
    }

    private void dismissDialogQuietly() {
        AlertDialog dialog = activeDialog;
        activeDialog = null;
        if (dialog != null) {
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception ignored) {
                // Activity 可能已销毁
            }
        }
    }
}
