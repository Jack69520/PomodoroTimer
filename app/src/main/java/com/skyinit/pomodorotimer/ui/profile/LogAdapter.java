package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.R;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private List<DevLabActivity.LogEntry> logEntries;

    public LogAdapter(List<DevLabActivity.LogEntry> logEntries) {
        this.logEntries = logEntries;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log_entry, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        DevLabActivity.LogEntry entry = logEntries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return logEntries.size();
    }

    public void updateLogs(List<DevLabActivity.LogEntry> newLogEntries) {
        this.logEntries = newLogEntries;
        notifyDataSetChanged();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTimestamp;
        private TextView tvLevel;
        private TextView tvTag;
        private TextView tvMessage;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvTag = itemView.findViewById(R.id.tv_tag);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }

        public void bind(DevLabActivity.LogEntry entry) {
            tvTimestamp.setText(entry.timestamp);
            tvLevel.setText(entry.level);
            tvTag.setText(entry.tag);
            tvMessage.setText(entry.message);

            int levelColor = getLevelColor(itemView.getContext(), entry.level);
            tvLevel.setTextColor(levelColor);
        }

        private int getLevelColor(Context context, String level) {
            String[] levels = context.getResources().getStringArray(R.array.dev_lab_log_levels);
            if (levels.length > 1 && level.equals(levels[1])) {
                return Color.RED;
            }
            if (levels.length > 2 && level.equals(levels[2])) {
                return Color.parseColor("#FFA500");
            }
            if (levels.length > 0 && level.equals(levels[0])) {
                return Color.BLUE;
            }
            if (levels.length > 3 && level.equals(levels[3])) {
                return Color.GRAY;
            }
            return Color.BLACK;
        }
    }
}
