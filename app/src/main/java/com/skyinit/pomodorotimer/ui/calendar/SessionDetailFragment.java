package com.skyinit.pomodorotimer.ui.calendar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.util.SessionPauseUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionDetailFragment extends Fragment {

    private static final String ARG_SESSION_ID = "sessionId";
    private static final int MAX_NOTES_LENGTH = 200;

    private SessionDetailViewModel viewModel;
    private int sessionId = -1;
    private PomodoroSession currentSession;

    private TextInputEditText notesInput;
    private TextView pauseReasonsText;

    public static SessionDetailFragment newInstance(int sessionId) {
        SessionDetailFragment fragment = new SessionDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getInt(ARG_SESSION_ID, -1);
        }

        viewModel = new ViewModelProvider(this,
                ((App) requireActivity().getApplication()).getContainer().getViewModelFactory())
                .get(SessionDetailViewModel.class);

        notesInput = view.findViewById(R.id.notes_input);
        pauseReasonsText = view.findViewById(R.id.pause_reasons_text);
        MaterialButton saveNotesBtn = view.findViewById(R.id.save_notes_btn);

        saveNotesBtn.setOnClickListener(v -> saveNotes());

        if (sessionId <= 0) {
            Toast.makeText(getContext(), R.string.session_detail_not_found, Toast.LENGTH_SHORT).show();
            navigateBackSafely();
            return;
        }

        viewModel.loadSession(sessionId);
        if (viewModel.getSessionSource() != null) {
            viewModel.getSessionSource().observe(getViewLifecycleOwner(), session -> {
                if (!viewModel.validateSession(session)) {
                    Toast.makeText(getContext(), R.string.session_detail_not_found, Toast.LENGTH_SHORT).show();
                    navigateBackSafely();
                    return;
                }
                bindSession(session);
            });
        }

        viewModel.getAccessDenied().observe(getViewLifecycleOwner(), denied -> {
            if (Boolean.TRUE.equals(denied) && isAdded()) {
                navigateBackSafely();
            }
        });

        viewModel.getSaveNotesResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.success && currentSession != null) {
                String notes = notesInput.getText() != null ? notesInput.getText().toString().trim() : "";
                currentSession.notes = notes;
                Toast.makeText(getContext(), R.string.session_detail_notes_saved, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindSession(PomodoroSession session) {
        currentSession = session;
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        setDetailRow(R.id.row_start_time, R.string.session_detail_start_time,
                dateTimeFormat.format(new Date(session.startTime)));
        setDetailRow(R.id.row_end_time, R.string.session_detail_end_time,
                session.endTime > 0
                        ? dateTimeFormat.format(new Date(session.endTime))
                        : getString(R.string.session_detail_none));
        setDetailRow(R.id.row_duration, R.string.session_detail_duration,
                formatDurationMinutes(session.duration));
        setDetailRow(R.id.row_category, R.string.session_detail_category,
                TextUtils.isEmpty(session.category) ? getString(R.string.session_detail_uncategorized) : session.category);
        setDetailRow(R.id.row_status, R.string.session_detail_completion_status,
                session.completed ? getString(R.string.session_detail_completed) : getString(R.string.session_detail_incomplete));

        boolean hasPause = SessionPauseUtils.hasPause(session.pauseCount, session.pauseReasons, session.pauseReason);
        setDetailRow(R.id.row_has_pause, R.string.session_detail_has_pause,
                hasPause ? getString(R.string.yes) : getString(R.string.no));

        int displayPauseCount = session.pauseCount;
        if (displayPauseCount <= 0 && hasPause) {
            List<String> reasons = SessionPauseUtils.decodeReasons(session.pauseReasons, session.pauseReason);
            displayPauseCount = reasons.size();
        }
        setDetailRow(R.id.row_pause_count, R.string.session_detail_pause_count,
                String.valueOf(displayPauseCount));

        List<String> reasons = SessionPauseUtils.decodeReasons(session.pauseReasons, session.pauseReason);
        if (reasons.isEmpty()) {
            pauseReasonsText.setText(R.string.session_detail_none);
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < reasons.size(); i++) {
                if (i > 0) {
                    builder.append('\n');
                }
                builder.append(i + 1).append(". ").append(reasons.get(i));
            }
            pauseReasonsText.setText(builder.toString());
        }

        boolean earlyEnd = session.earlyEnd || !session.completed;
        setDetailRow(R.id.row_early_end, R.string.session_detail_early_end,
                earlyEnd ? getString(R.string.yes) : getString(R.string.no));

        if (notesInput != null) {
            notesInput.setText(session.notes != null ? session.notes : "");
        }
    }

    private void setDetailRow(int rowId, int labelRes, String value) {
        View row = requireView().findViewById(rowId);
        if (row == null) {
            return;
        }
        TextView label = row.findViewById(R.id.detail_label);
        TextView detailValue = row.findViewById(R.id.detail_value);
        if (label != null) {
            label.setText(labelRes);
        }
        if (detailValue != null) {
            detailValue.setText(value);
        }
    }

    private String formatDurationMinutes(long durationMs) {
        long minutes = Math.max(0L, durationMs / (1000L * 60L));
        return getString(R.string.session_detail_duration_minutes, minutes);
    }

    private void saveNotes() {
        if (currentSession == null || notesInput == null) {
            return;
        }
        String notes = notesInput.getText() != null ? notesInput.getText().toString().trim() : "";
        if (notes.length() > MAX_NOTES_LENGTH) {
            Toast.makeText(getContext(), R.string.session_detail_notes_too_long, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.saveNotes(notes);
    }

    private void navigateBackSafely() {
        if (!isAdded()) {
            return;
        }
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }
}
