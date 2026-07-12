package com.skyinit.pomodorotimer.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SessionAppBlockRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class SessionBlockRecordAdapter extends RecyclerView.Adapter<SessionBlockRecordAdapter.ViewHolder> {

    private final List<SessionAppBlockRecord> records;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    SessionBlockRecordAdapter(List<SessionAppBlockRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_block_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SessionAppBlockRecord record = records.get(position);
        holder.sequenceText.setText(String.valueOf(record.sequenceNumber));
        holder.appNameText.setText(record.appName != null && !record.appName.isEmpty()
                ? record.appName
                : record.appPackageName);
        holder.blockTimeText.setText(timeFormat.format(new Date(record.blockTimeMillis)));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView sequenceText;
        final TextView appNameText;
        final TextView blockTimeText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            sequenceText = itemView.findViewById(R.id.sequence_text);
            appNameText = itemView.findViewById(R.id.app_name_text);
            blockTimeText = itemView.findViewById(R.id.block_time_text);
        }
    }
}
