package com.skyinit.pomodorotimer.ui.home;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.R;

import java.util.Random;

/**
 * 计时页 / 锁屏计时页共用的随机图片背景加载与回收。
 */
public final class TimerBackgroundHelper {

    private static final String TAG = "TimerBackground";

    private static final int[] TIMER_BACKGROUND_RES_IDS = {
            R.drawable.timerbackgroundimage_1,
            R.drawable.timerbackgroundimage_2,
            R.drawable.timerbackgroundimage_3,
            R.drawable.timerbackgroundimage_4,
            R.drawable.timerbackgroundimage_5,
            R.drawable.timerbackgroundimage_6,
            R.drawable.timerbackgroundimage_7,
            R.drawable.timerbackgroundimage_8,
            R.drawable.timerbackgroundimage_9,
            R.drawable.timerbackgroundimage_10,
            R.drawable.timerbackgroundimage_11,
            R.drawable.timerbackgroundimage_12
    };

    private TimerBackgroundHelper() {
    }

    public static void applyRandomBackground(@NonNull Context context, @Nullable ImageView bgImageView) {
        if (bgImageView == null || TIMER_BACKGROUND_RES_IDS.length == 0) {
            return;
        }
        try {
            int resId = TIMER_BACKGROUND_RES_IDS[new Random().nextInt(TIMER_BACKGROUND_RES_IDS.length)];
            if (loadBackgroundImage(context, bgImageView, resId)) {
                bgImageView.setVisibility(View.VISIBLE);
            } else {
                Log.w(TAG, "Failed to load timer background, using gradient fallback. resId=" + resId);
                bgImageView.setImageDrawable(null);
                bgImageView.setVisibility(View.GONE);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error setting timer background, using gradient fallback.", t);
            bgImageView.setImageDrawable(null);
            bgImageView.setVisibility(View.GONE);
        }
    }

    public static void recycle(@Nullable ImageView bgImageView) {
        if (bgImageView == null) {
            return;
        }
        try {
            Drawable drawable = bgImageView.getDrawable();
            bgImageView.setImageDrawable(null);
            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to recycle background bitmap", e);
        }
    }

    private static boolean loadBackgroundImage(@NonNull Context context,
                                               @NonNull ImageView bgImageView,
                                               int resId) {
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int targetW = Math.max(dm.widthPixels, 1);
        int targetH = Math.max(dm.heightPixels, 1);
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(resources, resId);
            Bitmap bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int iw = info.getSize().getWidth();
                int ih = info.getSize().getHeight();
                if (iw <= 0 || ih <= 0) {
                    return;
                }
                float scale = Math.min((float) targetW / iw, (float) targetH / ih);
                if (scale < 1f) {
                    decoder.setTargetSize(
                            Math.max(1, Math.round(iw * scale)),
                            Math.max(1, Math.round(ih * scale))
                    );
                }
            });
            if (bitmap != null) {
                bgImageView.setImageBitmap(bitmap);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "ImageDecoder failed for resId=" + resId, e);
        }
        try {
            bgImageView.setImageResource(resId);
            return bgImageView.getDrawable() != null;
        } catch (Exception e) {
            Log.w(TAG, "setImageResource failed for resId=" + resId, e);
            return false;
        }
    }
}
