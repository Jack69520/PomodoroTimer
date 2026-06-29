package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.repository.TaskRepository;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 待办集计时时选择子任务的对话框。
 */
public final class SubTaskTimerPickerDialog {

    public interface Listener {
        void onSubTaskSelected(TodoItem collection, SubTask subTask);
    }

    private SubTaskTimerPickerDialog() {
    }

    public static void show(Context context,
                            TodoItem collection,
                            TaskRepository taskRepository,
                            Listener listener) {
        taskRepository.runOnDisk(() -> {
            List<SubTask> subtasks = taskRepository.getSubtasksSync(collection.id);
            List<SubTask> available = new ArrayList<>();
            for (SubTask subTask : subtasks) {
                if (!subTask.completed) {
                    available.add(subTask);
                }
            }
            if (available.isEmpty()) {
                android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
                main.post(() -> android.widget.Toast.makeText(
                        context,
                        context.getString(com.skyinit.pomodorotimer.R.string.task_no_subtask_for_timer),
                        android.widget.Toast.LENGTH_SHORT).show());
                return;
            }
            String[] titles = new String[available.size()];
            for (int i = 0; i < available.size(); i++) {
                SubTask st = available.get(i);
                titles[i] = st.title + "  🍅" + st.completedPomodoros + "/" + Math.max(1, st.estimatedPomodoros);
            }
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            main.post(() -> new android.app.AlertDialog.Builder(context)
                    .setTitle(com.skyinit.pomodorotimer.R.string.task_select_subtask_timer)
                    .setItems(titles, (dialog, which) -> {
                        if (listener != null) {
                            listener.onSubTaskSelected(collection, available.get(which));
                        }
                    })
                    .show());
        });
    }
}
