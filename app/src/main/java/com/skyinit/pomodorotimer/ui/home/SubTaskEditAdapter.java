package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SubTaskEditAdapter extends RecyclerView.Adapter<SubTaskEditAdapter.ViewHolder> {

    public interface Listener {
        void onSubTaskToggle(SubTask subTask, boolean completed);
        void onSubTaskDelete(SubTask subTask);
        void onSubTaskPomodorosChanged(SubTask subTask, int estimatedPomodoros);
    }

    private List<SubTask> subTaskList = new ArrayList<>();
    private Listener listener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView order;
        CheckBox checkBox;
        TextView title;
        EditText estimatedPomodoros;
        TextView pomodoroProgress;

        ViewHolder(View view) {
            super(view);
            order = view.findViewById(R.id.subtask_order);
            checkBox = view.findViewById(R.id.subtask_checkbox);
            title = view.findViewById(R.id.subtask_title);
            estimatedPomodoros = view.findViewById(R.id.subtask_estimated_pomodoros);
            pomodoroProgress = view.findViewById(R.id.subtask_pomodoro_progress);
        }
    }

    public SubTaskEditAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtask_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubTask subTask = subTaskList.get(position);
        holder.order.setText(String.valueOf(subTask.order + 1));
        holder.title.setText(subTask.title);

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(subTask.completed);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onSubTaskToggle(subTask, isChecked);
            }
        });

        if (subTask.completed) {
            holder.title.setAlpha(0.6f);
        } else {
            holder.title.setAlpha(1.0f);
        }

        holder.estimatedPomodoros.setOnFocusChangeListener(null);
        Object tag = holder.estimatedPomodoros.getTag();
        if (tag instanceof TextWatcher) {
            holder.estimatedPomodoros.removeTextChangedListener((TextWatcher) tag);
        }
        holder.estimatedPomodoros.setText(String.valueOf(Math.max(1, subTask.estimatedPomodoros)));

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int value = Integer.parseInt(s.toString().trim());
                    if (listener != null) {
                        listener.onSubTaskPomodorosChanged(subTask, value);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        };
        holder.estimatedPomodoros.addTextChangedListener(watcher);
        holder.estimatedPomodoros.setTag(watcher);

        int completed = Math.max(0, subTask.completedPomodoros);
        int estimated = Math.max(1, subTask.estimatedPomodoros);
        holder.pomodoroProgress.setText(
                holder.itemView.getContext().getString(R.string.task_pomodoro_progress, completed, estimated));

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onSubTaskDelete(subTask);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return subTaskList.size();
    }

    public void setSubTasks(List<SubTask> subTasks) {
        this.subTaskList = subTasks != null ? new ArrayList<>(subTasks) : new ArrayList<>();
        notifyDataSetChanged();
    }
}
