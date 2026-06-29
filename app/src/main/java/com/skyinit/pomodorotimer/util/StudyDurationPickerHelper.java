package com.skyinit.pomodorotimer.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;

import com.skyinit.pomodorotimer.R;

/**
 * 学习时长选择弹窗，供首页长按与设置页共用。
 */
public final class StudyDurationPickerHelper {

    public interface OnDurationConfirmedListener {
        void onDurationConfirmed(int totalMinutes, long millis);
    }

    private StudyDurationPickerHelper() {
    }

    public static void show(Context context,
                            String title,
                            String positiveLabel,
                            long currentMillis,
                            OnDurationConfirmedListener listener) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null, false);
        NumberPicker hourPicker = content.findViewById(R.id.picker_hours);
        NumberPicker minutePicker = content.findViewById(R.id.picker_minutes);
        ViewGroup presetContainer = content.findViewById(R.id.preset_container);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(3);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        int currentTotalMinutes = (int) (currentMillis / 60_000L);
        hourPicker.setValue(Math.min(3, currentTotalMinutes / 60));
        minutePicker.setValue(currentTotalMinutes % 60);

        final AlertDialog[] dialogHolder = new AlertDialog[1];
        bindPresetButtons(context, presetContainer, hourPicker, minutePicker, dialogHolder, listener);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(content);
        builder.setPositiveButton(positiveLabel, (dialog, which) -> {
            int totalMinutes = hourPicker.getValue() * 60 + minutePicker.getValue();
            if (validateMinutes(context, totalMinutes, () ->
                    show(context, title, positiveLabel, currentMillis, listener))) {
                listener.onDurationConfirmed(totalMinutes, totalMinutes * 60_000L);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        dialogHolder[0] = builder.create();
        dialogHolder[0].show();
    }

    public static String formatDurationLabel(Context context, long millis) {
        long totalMinutes = millis / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0 && minutes > 0) {
            return context.getString(R.string.format_duration_hours_minutes, hours, minutes);
        }
        if (hours > 0) {
            return context.getString(R.string.format_duration_hours, hours);
        }
        return context.getString(R.string.format_duration_minutes, minutes);
    }

    private static void bindPresetButtons(Context context,
                                          ViewGroup presetContainer,
                                          NumberPicker hourPicker,
                                          NumberPicker minutePicker,
                                          AlertDialog[] dialogHolder,
                                          OnDurationConfirmedListener listener) {
        for (int i = 0; i < presetContainer.getChildCount(); i++) {
            View child = presetContainer.getChildAt(i);
            if (child instanceof Button) {
                child.setOnClickListener(v -> {
                    Object tag = v.getTag();
                    if (tag == null) {
                        return;
                    }
                    try {
                        int minutes = Integer.parseInt(tag.toString());
                        hourPicker.setValue(Math.min(3, minutes / 60));
                        minutePicker.setValue(minutes % 60);
                        if (validateMinutes(context, minutes, () -> {
                            if (dialogHolder[0] != null && dialogHolder[0].isShowing()) {
                                dialogHolder[0].dismiss();
                            }
                            show(context, context.getString(R.string.settings_study_duration),
                                    context.getString(R.string.confirm), minutes * 60_000L, listener);
                        })) {
                            listener.onDurationConfirmed(minutes, minutes * 60_000L);
                            if (dialogHolder[0] != null && dialogHolder[0].isShowing()) {
                                dialogHolder[0].dismiss();
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                });
            }
        }
    }

    private static boolean validateMinutes(Context context, int totalMinutes, Runnable retryAction) {
        if (totalMinutes <= 0) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.input_error_title)
                    .setMessage(R.string.study_duration_zero_error)
                    .setPositiveButton(R.string.got_it, (dialog, which) -> retryAction.run())
                    .setCancelable(false)
                    .show();
            return false;
        }
        if (totalMinutes > 180) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.study_duration_too_long_title)
                    .setMessage(R.string.study_duration_too_long_message)
                    .setPositiveButton(R.string.got_it, (dialog, which) -> retryAction.run())
                    .setCancelable(false)
                    .show();
            return false;
        }
        return true;
    }
}
