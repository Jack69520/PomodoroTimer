package com.skyinit.pomodorotimer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.ui.account.AccountRecoveryViewModel;
import com.skyinit.pomodorotimer.ui.account.AccountViewModel;
import com.skyinit.pomodorotimer.ui.account.ChangePasswordViewModel;
import com.skyinit.pomodorotimer.ui.account.LoginViewModel;
import com.skyinit.pomodorotimer.ui.account.RegisterViewModel;
import com.skyinit.pomodorotimer.ui.account.SetNewPasswordViewModel;
import com.skyinit.pomodorotimer.ui.calendar.CalendarViewModel;
import com.skyinit.pomodorotimer.ui.calendar.SessionDetailViewModel;
import com.skyinit.pomodorotimer.ui.home.HomeViewModel;
import com.skyinit.pomodorotimer.ui.home.TaskEditViewModel;
import com.skyinit.pomodorotimer.ui.home.TimerViewModel;
import com.skyinit.pomodorotimer.ui.profile.ProfileViewModel;
import com.skyinit.pomodorotimer.ui.profile.SettingsViewModel;
import com.skyinit.pomodorotimer.ui.statistics.StatisticsViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;

    public ViewModelFactory(AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(
                    container.getTimerSettingsRepository(),
                    container.getUserSessionRepository(),
                    container.getTimerStateRepository()
            );
        }
        if (modelClass.isAssignableFrom(TimerViewModel.class)) {
            return (T) new TimerViewModel(
                    container.getTimerSettingsRepository(),
                    container.getUserSessionRepository(),
                    container.getTimerStateRepository()
            );
        }
        if (modelClass.isAssignableFrom(CalendarViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new CalendarViewModel(
                    application,
                    container.getSessionRepository(),
                    container.getUserSessionRepository()
            );
        }
        if (modelClass.isAssignableFrom(StatisticsViewModel.class)) {
            return (T) new StatisticsViewModel(
                    container.getStatisticsRepository(),
                    container.getUserSessionRepository()
            );
        }
        if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new ProfileViewModel(
                    application,
                    container.getUserSessionRepository(),
                    container.getSettingsManager(),
                    container.getStatisticsRepository()
            );
        }
        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(
                    container.getUserPomodoroSettingsRepository(),
                    container.getTimerSettingsRepository());
        }
        if (modelClass.isAssignableFrom(SessionDetailViewModel.class)) {
            return (T) new SessionDetailViewModel(
                    container.getSessionRepository(),
                    container.getStatisticsRepository(),
                    container.getUserSessionRepository()
            );
        }
        if (modelClass.isAssignableFrom(AccountViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new AccountViewModel(
                    application,
                    container.getUserSessionRepository(),
                    container.getAccountOperationGuard());
        }
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new LoginViewModel(
                    application,
                    container.getUserSessionRepository(),
                    container.getAccountOperationGuard());
        }
        if (modelClass.isAssignableFrom(RegisterViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new RegisterViewModel(application, container.getUserSessionRepository());
        }
        if (modelClass.isAssignableFrom(ChangePasswordViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new ChangePasswordViewModel(application, container.getUserSessionRepository());
        }
        if (modelClass.isAssignableFrom(AccountRecoveryViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new AccountRecoveryViewModel(
                    application,
                    container.getUserSessionRepository(),
                    container.getAccountOperationGuard());
        }
        if (modelClass.isAssignableFrom(SetNewPasswordViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new SetNewPasswordViewModel(application, container.getUserSessionRepository());
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }

    /**
     * 任务编辑页需要 taskId / taskType 参数，使用专用 Factory。
     */
    public ViewModelProvider.Factory createTaskEditFactory(int taskId, int taskType) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(TaskEditViewModel.class)) {
                    Application application = (Application) container.getAppContext();
                    return (T) new TaskEditViewModel(
                            application,
                            container.getTaskRepository(),
                            container.getRecurringTaskManager(),
                            taskId,
                            taskType
                    );
                }
                throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
            }
        };
    }
}
