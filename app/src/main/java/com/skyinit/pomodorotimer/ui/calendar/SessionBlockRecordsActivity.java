package com.skyinit.pomodorotimer.ui.calendar;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.SessionAppBlockRecord;
import com.skyinit.pomodorotimer.ui.SubpageActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 单次番茄计时的应用拦截记录详情页。
 */
public class SessionBlockRecordsActivity extends SubpageActivity {

    public static final String EXTRA_SESSION_START_TIME = "extra_session_start_time";
    public static final String EXTRA_SESSION_END_TIME = "extra_session_end_time";

    private SessionBlockRecordsViewModel viewModel;
    private TextView sessionStartText;
    private TextView sessionEndText;
    private TextView encouragementText;
    private TextView emptyText;
    private RecyclerView recordsRecyclerView;
    private SessionBlockRecordAdapter adapter;
    private final List<SessionAppBlockRecord> records = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithSubpageChrome(R.layout.activity_session_block_records,
                R.string.session_block_records_title);

        initViews();

        long sessionStartTime = getIntent().getLongExtra(EXTRA_SESSION_START_TIME, 0L);
        long sessionEndTime = getIntent().getLongExtra(EXTRA_SESSION_END_TIME, 0L);
        if (sessionStartTime <= 0L) {
            Toast.makeText(this, R.string.session_detail_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(
                this,
                ((App) getApplication()).getContainer().getViewModelFactory()
        ).get(SessionBlockRecordsViewModel.class);

        adapter = new SessionBlockRecordAdapter(records);
        recordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recordsRecyclerView.setAdapter(adapter);

        viewModel.load(
                sessionStartTime,
                sessionEndTime,
                getString(R.string.session_block_encourage_zero),
                getString(R.string.session_block_encourage_low),
                getString(R.string.session_block_encourage_high)
        );

        viewModel.getUiState().observe(this, state -> {
            if (state == null) {
                return;
            }
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            sessionStartText.setText(dateTimeFormat.format(new Date(state.sessionStartTime)));
            if (state.sessionEndTime > 0L) {
                sessionEndText.setText(dateTimeFormat.format(new Date(state.sessionEndTime)));
            } else {
                sessionEndText.setText(R.string.session_detail_none);
            }
            encouragementText.setText(state.encouragementMessage);
            records.clear();
            records.addAll(state.records);
            adapter.notifyDataSetChanged();
            boolean hasRecords = !state.records.isEmpty();
            recordsRecyclerView.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
            emptyText.setVisibility(hasRecords ? View.GONE : View.VISIBLE);
        });

        viewModel.getAccessDenied().observe(this, denied -> {
            if (Boolean.TRUE.equals(denied)) {
                finish();
            }
        });
    }

    private void initViews() {
        sessionStartText = findViewById(R.id.session_start_text);
        sessionEndText = findViewById(R.id.session_end_text);
        encouragementText = findViewById(R.id.encouragement_text);
        emptyText = findViewById(R.id.empty_text);
        recordsRecyclerView = findViewById(R.id.records_recycler_view);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
