package com.skyinit.pomodorotimer.ui.calendar;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.ui.statistics.SessionAdapter;
import com.skyinit.pomodorotimer.R;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarFragment extends Fragment {
    private static final long ACTION_DEBOUNCE_MS = 500L;

    private NestedScrollView calendarScroll;
    private View calendarCard;
    private MonthCalendarView calendarView;
    private RecyclerView sessionsRecyclerView;
    private TextView selectedDateText;
    private TextView totalDurationText;
    private TextView totalSessionsText;
    private TextView backToTodayChip;
    private TextView recordsCountBadge;
    private LinearLayout sessionsEmptyLayout;
    private TextView sessionsEmptyTitle;
    private TextView sessionsEmptyMessage;
    private TextView startFocusBtn;
    private TextView pickDateBtn;
    private LinearLayout quickActionsLayout;

    private SessionAdapter sessionAdapter;
    private Calendar selectedDate = Calendar.getInstance();
    private CalendarViewModel viewModel;
    private long lastActionMs;
    private boolean navigating;

    private enum EmptyActionMode {
        TODAY,
        PAST,
        FUTURE
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        initViews(view);
        initViewModel();
        setupRecyclerView();
        setupCalendar();
        observeViewModel();

        return view;
    }

    private void initViews(View view) {
        calendarScroll = view.findViewById(R.id.calendar_scroll);
        calendarCard = view.findViewById(R.id.calendar_card);
        calendarView = view.findViewById(R.id.calendar_view);
        sessionsRecyclerView = view.findViewById(R.id.sessions_recycler);
        selectedDateText = view.findViewById(R.id.selected_date_text);
        totalDurationText = view.findViewById(R.id.total_duration);
        totalSessionsText = view.findViewById(R.id.total_sessions);
        backToTodayChip = view.findViewById(R.id.back_to_today_chip);
        recordsCountBadge = view.findViewById(R.id.records_count_badge);
        sessionsEmptyLayout = view.findViewById(R.id.sessions_empty_layout);
        sessionsEmptyTitle = view.findViewById(R.id.sessions_empty_title);
        sessionsEmptyMessage = view.findViewById(R.id.sessions_empty_message);
        startFocusBtn = view.findViewById(R.id.start_focus_btn);
        pickDateBtn = view.findViewById(R.id.pick_date_btn);
        quickActionsLayout = view.findViewById(R.id.quick_actions_layout);

        setupQuickActions();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this,
                ((App) requireActivity().getApplication()).getContainer().getViewModelFactory())
                .get(CalendarViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getSelectedDateLabel().observe(getViewLifecycleOwner(), text -> {
            if (selectedDateText != null && text != null) {
                selectedDateText.setText(text);
            }
        });

        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
                selectedDate = (Calendar) date.clone();
                updateBackToTodayVisibility();
            }
        });

        viewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
            List<PomodoroSession> safe = sessions == null ? new ArrayList<>() : sessions;
            if (sessionAdapter != null) {
                sessionAdapter.setSessions(safe);
            }
            updateRecordsCount(safe.size());
            if (safe.isEmpty()) {
                showEmptyStateForDate(selectedDate);
            } else {
                showSessionsList();
            }
        });

        viewModel.getDayStatistics().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) {
                return;
            }
            totalSessionsText.setText(getString(R.string.calendar_sessions_count, stats.completedSessions));
            totalDurationText.setText(formatDuration(stats.totalDurationMs));
        });

        viewModel.getHighlightedDates().observe(getViewLifecycleOwner(), dates -> {
            if (calendarView != null) {
                calendarView.setHighlightedDates(dates == null ? new java.util.HashSet<>() : dates);
            }
        });
    }

    private void setupCalendar() {
        calendarView.setSelectedDate(selectedDate);
        calendarView.setOnDateSelectedListener(date -> {
            selectedDate = date;
            viewModel.selectDate(date);
            updateBackToTodayVisibility();
        });
        calendarView.setOnMonthChangedListener((year, month) -> viewModel.loadHighlightedDates(year, month));

        viewModel.selectDate(selectedDate);
        viewModel.loadHighlightedDates(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH));
        updateBackToTodayVisibility();
    }

    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(new ArrayList<>());
        sessionAdapter.setOnItemClickListener(session -> {
            if (!tryConsumeAction() || navigating || !isAdded()) {
                return;
            }
            navigating = true;
            Bundle args = new Bundle();
            args.putInt("sessionId", session.id);
            try {
                Navigation.findNavController(requireView()).navigate(R.id.nav_session_detail, args);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                navigating = false;
            }
        });
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        sessionsRecyclerView.setAdapter(sessionAdapter);
        sessionsRecyclerView.setNestedScrollingEnabled(false);
        sessionsRecyclerView.setHasFixedSize(false);
        sessionsRecyclerView.setItemAnimator(null);
    }

    private void setupQuickActions() {
        backToTodayChip.setOnClickListener(v -> {
            if (!tryConsumeAction()) {
                return;
            }
            jumpToToday();
        });
    }

    private void jumpToToday() {
        Calendar today = Calendar.getInstance();
        selectedDate = today;
        calendarView.setSelectedDate(today);
        viewModel.selectDate(today);
        viewModel.loadHighlightedDates(today.get(Calendar.YEAR), today.get(Calendar.MONTH));
        updateBackToTodayVisibility();
        highlightCalendarForPick();
    }

    private void highlightCalendarForPick() {
        if (calendarScroll != null) {
            calendarScroll.smoothScrollTo(0, 0);
        }
        if (calendarCard == null) {
            return;
        }
        calendarCard.animate().cancel();
        calendarCard.setScaleX(1f);
        calendarCard.setScaleY(1f);
        calendarCard.animate()
                .scaleX(0.985f)
                .scaleY(0.985f)
                .setDuration(110)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> calendarCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
        calendarView.requestFocus();
    }

    private boolean tryConsumeAction() {
        long now = System.currentTimeMillis();
        if (now - lastActionMs < ACTION_DEBOUNCE_MS) {
            return false;
        }
        lastActionMs = now;
        return true;
    }

    private void updateBackToTodayVisibility() {
        if (backToTodayChip == null) {
            return;
        }
        boolean show = selectedDate != null && !isSameDay(selectedDate, Calendar.getInstance());
        backToTodayChip.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateRecordsCount(int count) {
        if (recordsCountBadge == null) {
            return;
        }
        if (count <= 0) {
            recordsCountBadge.setText(R.string.calendar_records_count_zero);
        } else {
            recordsCountBadge.setText(getString(R.string.calendar_records_count, count));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        navigating = false;
        viewModel.refresh();
        updateBackToTodayVisibility();
    }

    private String formatDuration(long durationMs) {
        long minutes = durationMs / (1000 * 60);
        long hours = minutes / 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return getString(R.string.calendar_total_duration_hours, hours, minutes);
        }
        return getString(R.string.calendar_total_duration_minutes, minutes);
    }

    private void showEmptyStateForDate(Calendar date) {
        App app = (App) requireActivity().getApplication();
        if (!app.getContainer().getUserSessionRepository().isLoggedIn()) {
            applyGuestAuthEmptyState();
            return;
        }
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        String title;
        String message;
        EmptyActionMode mode;

        if (isSameDay(date, today)) {
            title = getString(R.string.calendar_empty_today_title);
            message = getMotivationalMessage();
            mode = EmptyActionMode.TODAY;
        } else if (isFutureDay(date, today)) {
            title = getString(R.string.calendar_empty_future_title);
            message = getString(R.string.calendar_empty_future_message);
            mode = EmptyActionMode.FUTURE;
        } else if (isSameDay(date, yesterday)) {
            title = getString(R.string.calendar_empty_yesterday_title);
            message = getString(R.string.calendar_empty_yesterday_message);
            mode = EmptyActionMode.PAST;
        } else {
            title = getString(R.string.calendar_empty_past_title);
            message = getString(R.string.calendar_empty_past_message);
            mode = EmptyActionMode.PAST;
        }

        applyEmptyState(title, message, mode);
    }

    private void applyGuestAuthEmptyState() {
        if (sessionsRecyclerView != null) {
            sessionsRecyclerView.setVisibility(View.GONE);
        }
        if (sessionsEmptyLayout != null) {
            sessionsEmptyLayout.setVisibility(View.VISIBLE);
        }
        if (sessionsEmptyTitle != null) {
            sessionsEmptyTitle.setText(R.string.calendar_guest_title);
        }
        if (sessionsEmptyMessage != null) {
            sessionsEmptyMessage.setText(R.string.calendar_guest_message);
        }
        if (quickActionsLayout != null) {
            quickActionsLayout.setVisibility(View.VISIBLE);
        }
        if (totalDurationText != null) {
            totalDurationText.setText(R.string.calendar_guest_title);
        }
        if (totalSessionsText != null) {
            totalSessionsText.setText("");
        }
        startFocusBtn.setVisibility(View.VISIBLE);
        pickDateBtn.setVisibility(View.VISIBLE);
        startFocusBtn.setText(R.string.auth_gate_register);
        startFocusBtn.setOnClickListener(v -> {
            if (!tryConsumeAction()) {
                return;
            }
            startActivity(new android.content.Intent(requireContext(),
                    com.skyinit.pomodorotimer.ui.account.RegisterActivity.class));
        });
        pickDateBtn.setText(R.string.calendar_empty_action_login);
        pickDateBtn.setOnClickListener(v -> {
            if (!tryConsumeAction()) {
                return;
            }
            startActivity(new android.content.Intent(requireContext(),
                    com.skyinit.pomodorotimer.ui.account.LoginActivity.class));
        });
    }

    /** 按「日」比较，忽略时分秒。 */
    private boolean isFutureDay(Calendar date, Calendar today) {
        Calendar d = startOfDay(date);
        Calendar t = startOfDay(today);
        return d.after(t);
    }

    private Calendar startOfDay(Calendar source) {
        Calendar cal = (Calendar) source.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private void applyEmptyState(String title, String message, EmptyActionMode mode) {
        if (sessionsRecyclerView != null) {
            sessionsRecyclerView.setVisibility(View.GONE);
        }
        if (sessionsEmptyLayout != null) {
            sessionsEmptyLayout.setVisibility(View.VISIBLE);
        }
        if (sessionsEmptyTitle != null) {
            sessionsEmptyTitle.setText(title);
        }
        if (sessionsEmptyMessage != null) {
            sessionsEmptyMessage.setText(message);
        }
        if (quickActionsLayout != null) {
            quickActionsLayout.setVisibility(View.VISIBLE);
        }

        startFocusBtn.setVisibility(View.VISIBLE);
        pickDateBtn.setVisibility(View.VISIBLE);

        if (mode == EmptyActionMode.FUTURE) {
            // 防呆：未来日期不引导「去专注」
            startFocusBtn.setText(R.string.calendar_back_to_today);
            startFocusBtn.setContentDescription(getString(R.string.calendar_a11y_back_to_today));
            startFocusBtn.setOnClickListener(v -> {
                if (!tryConsumeAction()) {
                    return;
                }
                jumpToToday();
            });
            pickDateBtn.setText(R.string.calendar_empty_action_pick_date);
            pickDateBtn.setContentDescription(getString(R.string.calendar_a11y_pick_date));
            pickDateBtn.setOnClickListener(v -> {
                if (!tryConsumeAction()) {
                    return;
                }
                highlightCalendarForPick();
            });
            return;
        }

        startFocusBtn.setText(R.string.calendar_empty_action_focus);
        startFocusBtn.setContentDescription(getString(R.string.calendar_a11y_start_focus));
        startFocusBtn.setOnClickListener(v -> {
            if (!tryConsumeAction()) {
                return;
            }
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToHome();
            }
        });

        if (mode == EmptyActionMode.PAST) {
            pickDateBtn.setText(R.string.calendar_back_to_today);
            pickDateBtn.setContentDescription(getString(R.string.calendar_a11y_back_to_today));
            pickDateBtn.setOnClickListener(v -> {
                if (!tryConsumeAction()) {
                    return;
                }
                jumpToToday();
            });
        } else {
            pickDateBtn.setText(R.string.calendar_empty_action_pick_date);
            pickDateBtn.setContentDescription(getString(R.string.calendar_a11y_pick_date));
            pickDateBtn.setOnClickListener(v -> {
                if (!tryConsumeAction()) {
                    return;
                }
                highlightCalendarForPick();
            });
        }
    }

    private void showSessionsList() {
        if (sessionsRecyclerView != null) {
            sessionsRecyclerView.setVisibility(View.VISIBLE);
        }
        if (sessionsEmptyLayout != null) {
            sessionsEmptyLayout.setVisibility(View.GONE);
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String getMotivationalMessage() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 6) {
            return getString(R.string.calendar_empty_late_night);
        }
        if (hour < 12) {
            return getString(R.string.calendar_empty_first_pomodoro);
        }
        if (hour < 18) {
            return getString(R.string.calendar_empty_start_today);
        }
        return getString(R.string.calendar_empty_time_precious);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUIForThemeChange();
    }

    private void updateUIForThemeChange() {
        if (getView() == null) {
            return;
        }

        int primaryText = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int secondaryText = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        if (selectedDateText != null) {
            selectedDateText.setTextColor(primaryText);
        }
        if (totalDurationText != null) {
            totalDurationText.setTextColor(primaryText);
        }
        if (totalSessionsText != null) {
            totalSessionsText.setTextColor(primaryText);
        }
        if (sessionsEmptyTitle != null) {
            sessionsEmptyTitle.setTextColor(primaryText);
        }
        if (sessionsEmptyMessage != null) {
            sessionsEmptyMessage.setTextColor(secondaryText);
        }
        if (recordsCountBadge != null) {
            recordsCountBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand));
        }
        if (backToTodayChip != null) {
            backToTodayChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand));
        }
        if (sessionsRecyclerView != null && sessionAdapter != null) {
            sessionAdapter.notifyDataSetChanged();
        }
        if (calendarView != null) {
            calendarView.refreshTheme();
        }
    }
}
