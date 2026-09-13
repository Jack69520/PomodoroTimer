package com.skyinit.pomodorotimer.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 记录详情页：渲染 {@link SessionDetailUiState}，消费一次性 {@link SessionDetailEffect}。
 */
public class SessionDetailFragment extends Fragment {

    private static final String ARG_SESSION_ID = "sessionId";

    private SessionDetailViewModel viewModel;

    private NestedScrollView scrollView;
    private TextView statusChip;
    private TextView pauseReasonsText;
    private TextView blockEncouragementText;
    private TextInputLayout notesInputLayout;
    private TextInputEditText notesInput;
    private MaterialButton saveNotesBtn;
    private MaterialButton viewBlockRecordsBtn;

    private boolean notesDirty;
    private boolean bindingNotes;
    @Nullable
    private String boundNotesBaseline;
    private boolean openingBlockRecords;

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

        int sessionId = -1;
        if (getArguments() != null) {
            sessionId = getArguments().getInt(ARG_SESSION_ID, -1);
        }

        viewModel = new ViewModelProvider(this,
                ((App) requireActivity().getApplication()).getContainer().getViewModelFactory())
                .get(SessionDetailViewModel.class);

        bindViews(view);
        setupImeInsets(scrollView);
        setupNotesInput();
        setupActions();

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getEffects().observe(getViewLifecycleOwner(), this::handleEffect);

        viewModel.dispatch(SessionDetailIntent.load(sessionId));
    }

    private void bindViews(@NonNull View view) {
        scrollView = view.findViewById(R.id.session_detail_scroll);
        statusChip = view.findViewById(R.id.status_chip);
        pauseReasonsText = view.findViewById(R.id.pause_reasons_text);
        blockEncouragementText = view.findViewById(R.id.block_encouragement_text);
        notesInputLayout = view.findViewById(R.id.notes_input_layout);
        notesInput = view.findViewById(R.id.notes_input);
        saveNotesBtn = view.findViewById(R.id.save_notes_btn);
        viewBlockRecordsBtn = view.findViewById(R.id.view_block_records_btn);
    }

    private void setupImeInsets(@NonNull NestedScrollView content) {
        final int initialLeft = content.getPaddingLeft();
        final int initialTop = content.getPaddingTop();
        final int initialRight = content.getPaddingRight();
        final int initialBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(navigationBars.bottom, ime.bottom);
            v.setPadding(
                    initialLeft + Math.max(navigationBars.left, cutout.left),
                    initialTop,
                    initialRight + Math.max(navigationBars.right, cutout.right),
                    initialBottom + bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private void setupNotesInput() {
        if (notesInput == null) {
            return;
        }
        notesInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (bindingNotes) {
                    return;
                }
                String current = s != null ? s.toString() : "";
                notesDirty = boundNotesBaseline == null || !current.equals(boundNotesBaseline);
                updateSaveEnabled();
                if (notesInputLayout != null) {
                    notesInputLayout.setError(null);
                }
            }
        });
        notesInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void setupActions() {
        if (saveNotesBtn != null) {
            saveNotesBtn.setOnClickListener(v -> {
                if (!saveNotesBtn.isEnabled()) {
                    return;
                }
                hideKeyboard();
                String notes = notesInput != null && notesInput.getText() != null
                        ? notesInput.getText().toString()
                        : "";
                viewModel.dispatch(SessionDetailIntent.saveNotes(notes));
            });
        }
        if (viewBlockRecordsBtn != null) {
            viewBlockRecordsBtn.setOnClickListener(v -> {
                if (openingBlockRecords || !viewBlockRecordsBtn.isEnabled()) {
                    return;
                }
                openingBlockRecords = true;
                viewBlockRecordsBtn.postDelayed(() -> openingBlockRecords = false, 600L);
                viewModel.dispatch(SessionDetailIntent.openBlockRecords());
            });
        }
    }

    private void render(@Nullable SessionDetailUiState state) {
        if (state == null || !isAdded()) {
            return;
        }
        if (!state.hasData) {
            setInteractiveEnabled(false);
            return;
        }

        setInteractiveEnabled(true);
        applyStatusChip(state.completionStatus);

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        setDetailRow(R.id.row_start_time, R.string.session_detail_start_time,
                dateTimeFormat.format(new Date(state.startTime)));
        setDetailRow(R.id.row_end_time, R.string.session_detail_end_time,
                state.endTime > 0L
                        ? dateTimeFormat.format(new Date(state.endTime))
                        : getString(R.string.session_detail_none));
        setDetailRow(R.id.row_duration, R.string.session_detail_duration,
                formatDurationMinutes(state.durationMs));
        setDetailRow(R.id.row_category, R.string.session_detail_category,
                TextUtils.isEmpty(state.category)
                        ? getString(R.string.session_detail_uncategorized)
                        : state.category);

        setDetailRow(R.id.row_has_pause, R.string.session_detail_has_pause,
                state.hasPause ? getString(R.string.yes) : getString(R.string.no));
        setDetailRow(R.id.row_pause_count, R.string.session_detail_pause_count,
                String.valueOf(state.pauseCount));
        setDetailRow(R.id.row_early_end, R.string.session_detail_early_end,
                state.earlyEnd ? getString(R.string.yes) : getString(R.string.no));
        setDetailRow(R.id.row_block_count, R.string.session_detail_block_count,
                String.valueOf(state.blockEventCount));
        if (blockEncouragementText != null) {
            blockEncouragementText.setText(state.blockEncouragement);
            blockEncouragementText.setVisibility(
                    TextUtils.isEmpty(state.blockEncouragement) ? View.GONE : View.VISIBLE);
        }

        bindPauseReasons(state.pauseReasons);
        syncNotesFromState(state.notes);

        if (viewBlockRecordsBtn != null) {
            viewBlockRecordsBtn.setEnabled(state.canOpenBlockRecords && !state.saving);
        }
        if (saveNotesBtn != null) {
            saveNotesBtn.setText(state.saving
                    ? R.string.session_detail_saving
                    : R.string.session_detail_save_notes);
        }
        updateSaveEnabled(state);
    }

    private void syncNotesFromState(@NonNull String notes) {
        if (notesInput == null) {
            return;
        }
        if (notesDirty) {
            return;
        }
        if (notes.equals(boundNotesBaseline)
                && notes.equals(notesInput.getText() != null ? notesInput.getText().toString() : "")) {
            return;
        }
        bindingNotes = true;
        notesInput.setText(notes);
        if (notesInput.getText() != null) {
            notesInput.setSelection(notesInput.getText().length());
        }
        boundNotesBaseline = notes;
        notesDirty = false;
        bindingNotes = false;
    }

    private void bindPauseReasons(@NonNull List<String> reasons) {
        if (pauseReasonsText == null) {
            return;
        }
        if (reasons.isEmpty()) {
            pauseReasonsText.setText(R.string.session_detail_none);
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(i + 1).append(". ").append(reasons.get(i));
        }
        pauseReasonsText.setText(builder.toString());
    }

    private void applyStatusChip(@NonNull SessionDetailUiState.CompletionStatus status) {
        if (statusChip == null || getContext() == null) {
            return;
        }
        int bg;
        int color;
        int label;
        switch (status) {
            case COMPLETED:
                bg = R.drawable.bg_session_status_completed;
                color = R.color.session_status_completed;
                label = R.string.session_detail_completed;
                break;
            case FAILED:
                bg = R.drawable.bg_session_status_failed;
                color = R.color.session_status_failed;
                label = R.string.session_detail_failed;
                break;
            case INCOMPLETE:
            default:
                bg = R.drawable.bg_session_status_incomplete;
                color = R.color.session_status_incomplete;
                label = R.string.session_detail_incomplete;
                break;
        }
        statusChip.setBackgroundResource(bg);
        statusChip.setTextColor(ContextCompat.getColor(requireContext(), color));
        statusChip.setText(label);
    }

    private void updateSaveEnabled() {
        SessionDetailUiState state = viewModel != null ? viewModel.getUiState().getValue() : null;
        updateSaveEnabled(state);
    }

    private void updateSaveEnabled(@Nullable SessionDetailUiState state) {
        if (saveNotesBtn == null) {
            return;
        }
        boolean hasData = state != null && state.hasData;
        boolean saving = state != null && state.saving;
        saveNotesBtn.setEnabled(hasData && !saving && notesDirty);
        saveNotesBtn.setAlpha(saveNotesBtn.isEnabled() ? 1f : 0.45f);
    }

    private void setInteractiveEnabled(boolean enabled) {
        if (notesInput != null) {
            notesInput.setEnabled(enabled);
        }
        if (viewBlockRecordsBtn != null) {
            viewBlockRecordsBtn.setEnabled(enabled);
        }
        if (!enabled && saveNotesBtn != null) {
            saveNotesBtn.setEnabled(false);
            saveNotesBtn.setAlpha(0.45f);
        }
    }

    private void handleEffect(@Nullable SessionDetailEffect effect) {
        if (effect == null || !isAdded()) {
            return;
        }
        switch (effect.type) {
            case SHOW_TOAST:
                if (effect.toastRes != 0) {
                    Toast.makeText(requireContext(), effect.toastRes, Toast.LENGTH_SHORT).show();
                }
                if (effect.toastRes == R.string.session_detail_notes_saved) {
                    markNotesCleanFromInput();
                }
                break;
            case NAVIGATE_BACK:
                if (effect.toastRes != 0) {
                    Toast.makeText(requireContext(), effect.toastRes, Toast.LENGTH_SHORT).show();
                }
                navigateBackSafely();
                break;
            case OPEN_BLOCK_RECORDS:
                openBlockRecords(effect.sessionStartTime, effect.sessionEndTime);
                break;
            default:
                break;
        }
    }

    private void markNotesCleanFromInput() {
        if (notesInput == null) {
            return;
        }
        String notes = notesInput.getText() != null ? notesInput.getText().toString() : "";
        boundNotesBaseline = notes;
        notesDirty = false;
        if (notesInputLayout != null) {
            notesInputLayout.setError(null);
        }
        updateSaveEnabled();
    }

    private void openBlockRecords(long startTime, long endTime) {
        if (getActivity() == null || startTime <= 0L) {
            return;
        }
        Intent intent = new Intent(getActivity(), SessionBlockRecordsActivity.class);
        intent.putExtra(SessionBlockRecordsActivity.EXTRA_SESSION_START_TIME, startTime);
        intent.putExtra(SessionBlockRecordsActivity.EXTRA_SESSION_END_TIME, endTime);
        startActivity(intent);
    }

    private void setDetailRow(int rowId, int labelRes, String value) {
        View root = getView();
        if (root == null) {
            return;
        }
        View row = root.findViewById(rowId);
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

    private void hideKeyboard() {
        if (notesInput == null || getContext() == null) {
            return;
        }
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(notesInput.getWindowToken(), 0);
        }
        notesInput.clearFocus();
    }

    private void navigateBackSafely() {
        if (!isAdded()) {
            return;
        }
        hideKeyboard();
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }

    @Override
    public void onPause() {
        // 离开页时收起键盘，避免返回列表后 IME 残留
        hideKeyboard();
        super.onPause();
    }
}
