package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SubTask;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.ViewHolder> {
    private List<SubTask> subTaskList;
    private OnSubTaskClickListener listener;

    public interface OnSubTaskClickListener {
        void onSubTaskClick(SubTask subTask);
        void onSubTaskToggle(SubTask subTask, boolean isChecked);
        void onSubTaskDelete(SubTask subTask);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public CheckBox checkBox;
        public TextView order;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.subtask_title);
            checkBox = view.findViewById(R.id.subtask_checkbox);
            order = view.findViewById(R.id.subtask_order);
        }
    }

    public SubTaskAdapter(List<SubTask> subTaskList, OnSubTaskClickListener listener) {
        this.subTaskList = subTaskList != null ? subTaskList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtask, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubTask subTask = subTaskList.get(position);
        holder.title.setText(subTask.title);
        holder.checkBox.setChecked(subTask.completed);
        holder.order.setText(String.valueOf(subTask.order + 1));

        // 设置完成状态的样式
        if (subTask.completed) {
            holder.title.setAlpha(0.6f);
            holder.title.setTextColor(0xFF888888);
        } else {
            holder.title.setAlpha(1.0f);
            holder.title.setTextColor(0xFFFFFFFF);
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(subTask.completed);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onSubTaskToggle(subTask, isChecked);
        });

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
        this.subTaskList = subTasks != null ? subTasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public SubTask getSubTaskAt(int position) {
        if (position >= 0 && position < subTaskList.size()) {
            return subTaskList.get(position);
        }
        return null;
    }
}
