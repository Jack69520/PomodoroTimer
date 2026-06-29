package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.StudyDurationPickerHelper;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

/**
 * 主页计时相关 UI：时长选择对话框与任务/子任务计时启动。
 */
public class HomeTimerUiHelper {

    public interface Host {
        Fragment getFragment();

        HomeViewModel getViewModel();

        TimerService getTimerService();

        void onDefaultStudyTimeChanged(long millis);
    }

    private final Host host;

    public HomeTimerUiHelper(Host host) {
        this.host = host;
    }

    public void showDefaultTimePickerDialog() {
        TimerService timerService = host.getTimerService();
        if (timerService != null && (timerService.isRunning() || timerService.isPaused())) {
            Toast.makeText(host.getFragment().requireContext(), R.string.home_toast_stop_timer_first, Toast.LENGTH_SHORT).show();
            return;
        }
        StudyDurationPickerHelper.show(
                host.getFragment().requireContext(),
                host.getFragment().getString(R.string.settings_study_duration),
                host.getFragment().getString(R.string.confirm),
                host.getViewModel().getDefaultStudyTimeMs(),
                (totalMinutes, millis) -> {
                    host.getViewModel().setDefaultStudyTime(millis);
                    host.onDefaultStudyTimeChanged(millis);
                    resetTimerServiceQuietly();
                });
    }

    public void showTaskTimePickerDialog(TodoItem item) {
        if (item.isCollection()) {
            Toast.makeText(host.getFragment().requireContext(),
                    R.string.task_collection_no_timer, Toast.LENGTH_SHORT).show();
            return;
        }
        showDurationPickerAndStart(item, null);
    }

    public void showSubTaskTimePickerDialog(TodoItem collection, SubTask subTask) {
        showDurationPickerAndStart(collection, subTask);
    }

    private void showDurationPickerAndStart(TodoItem item, SubTask subTask) {
        TimerService timerService = host.getTimerService();
        if (timerService != null && (timerService.isRunning() || timerService.isPaused())) {
            Toast.makeText(host.getFragment().requireContext(), R.string.home_toast_stop_current_timer, Toast.LENGTH_SHORT).show();
            return;
        }

        StudyDurationPickerHelper.show(
                host.getFragment().requireContext(),
                host.getFragment().getString(R.string.home_dialog_start_timer_title),
                host.getFragment().getString(R.string.start_button),
                host.getViewModel().getDefaultStudyTimeMs(),
                (totalMinutes, millis) -> {
                    if (subTask != null) {
                        startTimerForSubTask(item, subTask, millis);
                    } else {
                        startTimerForTask(item, millis);
                    }
                });
    }

    private void startTimerForTask(TodoItem item, long durationMs) {
        long clamped = clampDuration(durationMs);
        host.getViewModel().setDefaultStudyTime(clamped);
        host.onDefaultStudyTimeChanged(clamped);

        Intent intent = new Intent(host.getFragment().requireContext(), TimerActivity.class);
        intent.putExtra("start", true);
        intent.putExtra(TimerService.EXTRA_TASK_ID, item.id);
        intent.putExtra(TimerService.EXTRA_TASK_TITLE, item.title);
        intent.putExtra(TimerService.EXTRA_TASK_CATEGORY, item.category);
        intent.putExtra(TimerService.EXTRA_TASK_TAGS, item.tags != null ? item.tags : "");
        intent.putExtra(TimerService.EXTRA_SUB_TASK_ID, -1);
        host.getFragment().startActivity(intent);
    }

    private void startTimerForSubTask(TodoItem collection, SubTask subTask, long durationMs) {
        long clamped = clampDuration(durationMs);
        host.getViewModel().setDefaultStudyTime(clamped);
        host.onDefaultStudyTimeChanged(clamped);

        String displayTitle = collection.title + " › " + subTask.title;
        Intent intent = new Intent(host.getFragment().requireContext(), TimerActivity.class);
        intent.putExtra("start", true);
        intent.putExtra(TimerService.EXTRA_TASK_ID, collection.id);
        intent.putExtra(TimerService.EXTRA_TASK_TITLE, displayTitle);
        intent.putExtra(TimerService.EXTRA_TASK_CATEGORY, collection.category);
        intent.putExtra(TimerService.EXTRA_TASK_TAGS, collection.tags != null ? collection.tags : "");
        intent.putExtra(TimerService.EXTRA_SUB_TASK_ID, subTask.id);
        intent.putExtra(TimerService.EXTRA_SUB_TASK_TITLE, subTask.title);
        host.getFragment().startActivity(intent);
    }

    private long clampDuration(long durationMs) {
        long clamped = Math.min(durationMs, 180L * 60L * 1000L);
        if (clamped <= 0) {
            clamped = 25L * 60L * 1000L;
        }
        return clamped;
    }

    private void resetTimerServiceQuietly() {
        TimerService timerService = host.getTimerService();
        if (timerService != null) {
            timerService.resetTimer();
        }
    }
}
