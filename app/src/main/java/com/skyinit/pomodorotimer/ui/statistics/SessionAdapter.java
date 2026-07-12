package com.skyinit.pomodorotimer.ui.statistics;

import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;
import com.skyinit.pomodorotimer.util.TaskCategoryStyle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
    private List<PomodoroSession> sessionList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(PomodoroSession session);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView timeText;
        public TextView durationText;
        public TextView categoryText;
        public TextView statusText;

        public ViewHolder(View view) {
            super(view);
            timeText = view.findViewById(R.id.session_time);
            durationText = view.findViewById(R.id.session_duration);
            categoryText = view.findViewById(R.id.session_category);
            statusText = view.findViewById(R.id.session_status);
        }
    }

    public SessionAdapter(List<PomodoroSession> sessionList) {
        this.sessionList = sessionList != null ? sessionList : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PomodoroSession session = sessionList.get(position);
        android.content.Context context = holder.itemView.getContext();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(new Date(session.startTime));
        holder.timeText.setText(timeStr);

        long minutes = session.duration / (1000 * 60);
        long hours = minutes / 60;
        minutes = minutes % 60;

        String durationStr;
        if (hours > 0) {
            durationStr = context.getString(R.string.format_duration_hours_minutes, hours, minutes);
        } else {
            durationStr = context.getString(R.string.format_duration_minutes, minutes);
        }
        holder.durationText.setText(durationStr);

        if (session.category != null && !session.category.isEmpty()) {
            holder.categoryText.setText(session.category);
            TaskCategoryStyle.apply(holder.categoryText, session.category);
            holder.categoryText.setVisibility(View.VISIBLE);
        } else {
            holder.categoryText.setText(R.string.session_detail_uncategorized);
            TaskCategoryStyle.apply(holder.categoryText, null);
            holder.categoryText.setVisibility(View.VISIBLE);
        }

        if (session.completed) {
            holder.statusText.setText(R.string.statistics_status_completed);
            holder.statusText.setTextColor(0xFF4CAF50);
        } else if (SessionPauseUtils.isTimeoutFailure(
                session.pauseReason,
                session.pauseReasons,
                context.getString(R.string.timer_pause_reason_timeout))) {
            holder.statusText.setText(R.string.statistics_status_failed);
            holder.statusText.setTextColor(0xFFF44336);
        } else {
            holder.statusText.setText(R.string.statistics_status_incomplete);
            holder.statusText.setTextColor(0xFFFF9800);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public void setSessions(List<PomodoroSession> sessions) {
        this.sessionList = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }
}
