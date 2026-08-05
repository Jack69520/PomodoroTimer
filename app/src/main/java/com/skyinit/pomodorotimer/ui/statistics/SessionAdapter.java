package com.skyinit.pomodorotimer.ui.statistics;

import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;
import com.skyinit.pomodorotimer.util.TaskCategoryStyle;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
    private List<PomodoroSession> sessionList;
    private OnItemClickListener onItemClickListener;
    private long lastClickMs;

    public interface OnItemClickListener {
        void onItemClick(PomodoroSession session);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View statusIndicator;
        public TextView timeText;
        public TextView durationText;
        public TextView timeRangeText;
        public TextView categoryText;
        public TextView statusText;
        public TextView notesBadge;

        public ViewHolder(View view) {
            super(view);
            statusIndicator = view.findViewById(R.id.session_status_indicator);
            timeText = view.findViewById(R.id.session_time);
            durationText = view.findViewById(R.id.session_duration);
            timeRangeText = view.findViewById(R.id.session_time_range);
            categoryText = view.findViewById(R.id.session_category);
            statusText = view.findViewById(R.id.session_status);
            notesBadge = view.findViewById(R.id.session_notes_badge);
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
        String startStr = timeFormat.format(new Date(session.startTime));
        holder.timeText.setText(startStr);

        long endTime = session.endTime > 0
                ? session.endTime
                : session.startTime + Math.max(0L, session.duration);
        String endStr = timeFormat.format(new Date(endTime));
        holder.timeRangeText.setText(
                context.getString(R.string.calendar_session_time_range, startStr, endStr));

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
        } else {
            holder.categoryText.setText(R.string.session_detail_uncategorized);
            TaskCategoryStyle.apply(holder.categoryText, null);
        }
        holder.categoryText.setVisibility(View.VISIBLE);

        boolean hasNotes = session.notes != null && !session.notes.trim().isEmpty();
        holder.notesBadge.setVisibility(hasNotes ? View.VISIBLE : View.GONE);

        String statusLabel;
        int statusColor;
        int indicatorColor;
        int statusBg;
        if (session.completed) {
            statusLabel = context.getString(R.string.statistics_status_completed);
            statusColor = ContextCompat.getColor(context, R.color.session_status_completed);
            indicatorColor = ContextCompat.getColor(context, R.color.calendar_indicator_completed);
            statusBg = R.drawable.bg_session_status_completed;
        } else if (SessionPauseUtils.isTimeoutFailure(
                session.pauseReason,
                session.pauseReasons,
                context.getString(R.string.timer_pause_reason_timeout))) {
            statusLabel = context.getString(R.string.statistics_status_failed);
            statusColor = ContextCompat.getColor(context, R.color.session_status_failed);
            indicatorColor = ContextCompat.getColor(context, R.color.calendar_indicator_failed);
            statusBg = R.drawable.bg_session_status_failed;
        } else {
            statusLabel = context.getString(R.string.statistics_status_incomplete);
            statusColor = ContextCompat.getColor(context, R.color.session_status_incomplete);
            indicatorColor = ContextCompat.getColor(context, R.color.calendar_indicator_incomplete);
            statusBg = R.drawable.bg_session_status_incomplete;
        }
        holder.statusText.setText(statusLabel);
        holder.statusText.setTextColor(statusColor);
        holder.statusText.setBackgroundResource(statusBg);
        GradientDrawable indicator = (GradientDrawable) ContextCompat
                .getDrawable(context, R.drawable.bg_session_status_indicator)
                .mutate();
        indicator.setColor(indicatorColor);
        holder.statusIndicator.setBackground(indicator);

        holder.itemView.setContentDescription(context.getString(
                R.string.calendar_a11y_session_item,
                startStr,
                durationStr,
                holder.categoryText.getText(),
                statusLabel));

        holder.itemView.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastClickMs < 450L) {
                return;
            }
            lastClickMs = now;
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
