package com.skyinit.pomodorotimer.ui.profile.devlab;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.R;

/**
 * 开发实验室日志列表：DiffUtil 增量刷新。
 */
public class DevLabLogAdapter extends ListAdapter<DevLabLogEntry, DevLabLogAdapter.LogViewHolder> {

    public DevLabLogAdapter() {
        super(DIFF);
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
        holder.bind(getItem(position));
    }

    static final class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTimestamp;
        private final TextView tvLevel;
        private final TextView tvTag;
        private final TextView tvMessage;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvTag = itemView.findViewById(R.id.tv_tag);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }

        void bind(@NonNull DevLabLogEntry entry) {
            Context context = itemView.getContext();
            tvTimestamp.setText(entry.timestamp);
            tvLevel.setText(entry.level.labelRes);
            tvTag.setText(entry.tag);
            tvMessage.setText(entry.message);

            int color = levelColor(context, entry.level);
            tvLevel.setTextColor(ContextCompat.getColor(context, R.color.absolute_white));
            tvLevel.setBackgroundTintList(ColorStateList.valueOf(color));
        }

        private static int levelColor(@NonNull Context context, @NonNull DevLabLogLevel level) {
            switch (level) {
                case ERROR:
                    return ContextCompat.getColor(context, R.color.semantic_error);
                case WARNING:
                    return ContextCompat.getColor(context, R.color.semantic_warning);
                case DEBUG:
                    return ContextCompat.getColor(context, R.color.text_hint);
                case INFO:
                default:
                    return ContextCompat.getColor(context, R.color.brand);
            }
        }
    }

    private static final DiffUtil.ItemCallback<DevLabLogEntry> DIFF =
            new DiffUtil.ItemCallback<DevLabLogEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull DevLabLogEntry oldItem,
                                               @NonNull DevLabLogEntry newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DevLabLogEntry oldItem,
                                                  @NonNull DevLabLogEntry newItem) {
                    return oldItem.timestamp.equals(newItem.timestamp)
                            && oldItem.level == newItem.level
                            && oldItem.tag.equals(newItem.tag)
                            && oldItem.message.equals(newItem.message);
                }
            };
}
