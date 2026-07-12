package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.skyinit.pomodorotimer.R;

/**
 * 专注模式遮罩：在用户打开被屏蔽应用时显示全屏提示并引导返回对应页面。
 * 不使用 AccessibilityService，符合常见上架策略。
 */
public final class FocusBlockOverlayManager {

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlayView;
    private boolean isShowing;

    public FocusBlockOverlayManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public boolean canShowOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(appContext);
        }
        return true;
    }

    public void show(String blockedAppLabel) {
        if (!canShowOverlay() || isShowing) {
            return;
        }
        mainHandler.post(() -> {
            if (isShowing) {
                return;
            }
            try {
                if (windowManager == null) {
                    windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
                }
                overlayView = LayoutInflater.from(appContext).inflate(R.layout.overlay_focus_block, null);
                TextView messageView = overlayView.findViewById(R.id.overlay_message);
                if (messageView != null && blockedAppLabel != null && !blockedAppLabel.isEmpty()) {
                    messageView.setText(appContext.getString(R.string.focus_block_overlay_message_with_app, blockedAppLabel));
                }
                View returnButton = overlayView.findViewById(R.id.overlay_return_button);
                if (returnButton != null) {
                    returnButton.setOnClickListener(v -> openTimerAndHide());
                }
                overlayView.setOnClickListener(v -> openTimerAndHide());

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                : WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        PixelFormat.TRANSLUCENT
                );
                params.gravity = Gravity.CENTER;
                windowManager.addView(overlayView, params);
                isShowing = true;
            } catch (Exception e) {
                AppLog.e("FocusBlockOverlay", "Failed to show overlay", e);
                hideInternal();
            }
        });
    }

    public void hide() {
        mainHandler.post(this::hideInternal);
    }

    private void openTimerAndHide() {
        try {
            FocusBlockNavigation.openReturnDestination(appContext);
        } catch (Exception e) {
            AppLog.e("FocusBlockOverlay", "Failed to open return destination", e);
        }
        hideInternal();
    }

    private void hideInternal() {
        if (!isShowing || overlayView == null || windowManager == null) {
            isShowing = false;
            overlayView = null;
            return;
        }
        try {
            windowManager.removeView(overlayView);
        } catch (Exception e) {
            AppLog.w("FocusBlockOverlay", "Overlay remove failed", e);
        }
        overlayView = null;
        isShowing = false;
    }
}
