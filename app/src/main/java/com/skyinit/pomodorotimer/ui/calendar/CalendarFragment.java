package com.skyinit.pomodorotimer.ui.calendar;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.ui.account.LoginActivity;
import com.skyinit.pomodorotimer.ui.statistics.SessionAdapter;
import com.skyinit.pomodorotimer.R;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class CalendarFragment extends Fragment {
    private MonthCalendarView calendarView;
    private RecyclerView sessionsRecyclerView;
    private TextView selectedDateText;
    private TextView totalDurationText;
    private TextView totalSessionsText;
    private ScrollView sessionsEmptyScroll;
    private LinearLayout sessionsEmptyLayout;
    private ImageView emptyStateIcon;
    private TextView sessionsEmptyTitle;
    private TextView sessionsEmptyMessage;
    private TextView startFocusBtn;
    private TextView loginBtn;

    private SessionAdapter sessionAdapter;
    private Calendar selectedDate = Calendar.getInstance();
    private CalendarViewModel viewModel;

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
        calendarView = view.findViewById(R.id.calendar_view);
        sessionsRecyclerView = view.findViewById(R.id.sessions_recycler);
        selectedDateText = view.findViewById(R.id.selected_date_text);
        totalDurationText = view.findViewById(R.id.total_duration);
        totalSessionsText = view.findViewById(R.id.total_sessions);
        sessionsEmptyScroll = view.findViewById(R.id.sessions_empty_scroll);
        sessionsEmptyLayout = view.findViewById(R.id.sessions_empty_layout);
        emptyStateIcon = view.findViewById(R.id.empty_state_icon);
        sessionsEmptyTitle = view.findViewById(R.id.sessions_empty_title);
        sessionsEmptyMessage = view.findViewById(R.id.sessions_empty_message);
        startFocusBtn = view.findViewById(R.id.start_focus_btn);
        loginBtn = view.findViewById(R.id.login_btn);

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

        viewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessionAdapter != null) {
                sessionAdapter.setSessions(sessions == null ? new ArrayList<>() : sessions);
            }
            if (sessions == null || sessions.isEmpty()) {
                Calendar date = selectedDate;
                if (date != null) {
                    showEmptyStateForDate(date);
                }
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
        });
        calendarView.setOnMonthChangedListener((year, month) -> viewModel.loadHighlightedDates(year, month));

        viewModel.selectDate(selectedDate);
        viewModel.loadHighlightedDates(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH));
    }

    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(new ArrayList<>());
        sessionAdapter.setOnItemClickListener(session -> {
            Bundle args = new Bundle();
            args.putInt("sessionId", session.id);
            Navigation.findNavController(requireView()).navigate(R.id.nav_session_detail, args);
        });
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sessionsRecyclerView.setAdapter(sessionAdapter);
    }

    private void setupQuickActions() {
        startFocusBtn.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToHome();
            }
        });

        loginBtn.setOnClickListener(v -> {
            if (getString(R.string.calendar_empty_action_pick_date).contentEquals(loginBtn.getText())) {
                calendarView.requestFocus();
            } else {
                startActivity(new Intent(getActivity(), LoginActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
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

    private void showEmptyState(String title, String message, String buttonText) {
        if (sessionsRecyclerView != null) {
            sessionsRecyclerView.setVisibility(View.GONE);
        }
        if (sessionsEmptyScroll != null) {
            sessionsEmptyScroll.setVisibility(View.VISIBLE);
        }
        if (sessionsEmptyLayout != null) {
            if (sessionsEmptyTitle != null) {
                sessionsEmptyTitle.setText(title);
            }
            if (sessionsEmptyMessage != null) {
                sessionsEmptyMessage.setText(message);
            }
            if (startFocusBtn != null) {
                startFocusBtn.setText(buttonText);
            }
        }
    }

    private void showEmptyStateWithTwoButtons(String title, String message, String leftButtonText, String rightButtonText) {
        if (sessionsRecyclerView != null) {
            sessionsRecyclerView.setVisibility(View.GONE);
        }
        if (sessionsEmptyScroll != null) {
            sessionsEmptyScroll.setVisibility(View.VISIBLE);
        }
        if (sessionsEmptyLayout != null) {
            if (sessionsEmptyTitle != null) {
                sessionsEmptyTitle.setText(title);
            }
            if (sessionsEmptyMessage != null) {
                sessionsEmptyMessage.setText(message);
            }
            if (startFocusBtn != null) {
                startFocusBtn.setText(leftButtonText);
            }
            if (loginBtn != null) {
                loginBtn.setText(rightButtonText);
            }
        }
    }

    private void showEmptyStateForDate(Calendar date) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        String title;
        String message;

        if (isSameDay(date, today)) {
            title = getString(R.string.calendar_empty_today_title);
            message = getMotivationalMessage();
        } else if (isSameDay(date, yesterday)) {
            title = getString(R.string.calendar_empty_yesterday_title);
            message = getString(R.string.calendar_empty_yesterday_message);
        } else if (date.before(today)) {
            title = getString(R.string.calendar_empty_past_title);
            message = getString(R.string.calendar_empty_past_message);
        } else {
            title = getString(R.string.calendar_empty_future_title);
            message = getString(R.string.calendar_empty_future_message);
        }

        showEmptyStateWithTwoButtons(title, message,
                getString(R.string.calendar_empty_action_focus),
                getString(R.string.calendar_empty_action_pick_date));
    }

    private void showSessionsList() {
        if (sessionsRecyclerView != null) {
            sessionsRecyclerView.setVisibility(View.VISIBLE);
        }
        if (sessionsEmptyScroll != null) {
            sessionsEmptyScroll.setVisibility(View.GONE);
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String getMotivationalMessage() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);

        if (hour < 6) {
            return getString(R.string.calendar_empty_late_night);
        } else if (hour < 12) {
            return getString(R.string.calendar_empty_first_pomodoro);
        } else if (hour < 18) {
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

        if (selectedDateText != null) {
            selectedDateText.setTextColor(getResources().getColor(R.color.text_primary));
        }
        if (totalDurationText != null) {
            totalDurationText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (totalSessionsText != null) {
            totalSessionsText.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        if (sessionsEmptyTitle != null) {
            sessionsEmptyTitle.setTextColor(getResources().getColor(R.color.text_primary));
        }
        if (sessionsEmptyMessage != null) {
            sessionsEmptyMessage.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        if (sessionsRecyclerView != null && sessionAdapter != null) {
            sessionAdapter.notifyDataSetChanged();
        }
    }
}
