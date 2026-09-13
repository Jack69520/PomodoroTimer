package com.skyinit.pomodorotimer.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.ui.account.AccountRecoveryViewModel;
import com.skyinit.pomodorotimer.ui.account.AccountSecurityViewModel;
import com.skyinit.pomodorotimer.ui.account.AccountViewModel;
import com.skyinit.pomodorotimer.ui.account.ChangePasswordViewModel;
import com.skyinit.pomodorotimer.ui.account.EditNicknameViewModel;
import com.skyinit.pomodorotimer.ui.account.EditSignatureViewModel;
import com.skyinit.pomodorotimer.ui.account.LoginViewModel;
import com.skyinit.pomodorotimer.ui.account.RegisterViewModel;
import com.skyinit.pomodorotimer.ui.account.SetNewPasswordViewModel;
import com.skyinit.pomodorotimer.ui.calendar.CalendarViewModel;
import com.skyinit.pomodorotimer.ui.calendar.SessionBlockRecordsViewModel;
import com.skyinit.pomodorotimer.ui.calendar.SessionDetailViewModel;
import com.skyinit.pomodorotimer.ui.home.HomeViewModel;
import com.skyinit.pomodorotimer.ui.home.TimerViewModel;
import com.skyinit.pomodorotimer.ui.home.todo.HomeTodoViewModel;
import com.skyinit.pomodorotimer.ui.home.todoedit.TaskEditViewModel;
import com.skyinit.pomodorotimer.ui.onboarding.FirstRegisterViewModel;
import com.skyinit.pomodorotimer.ui.profile.AppBlockingViewModel;
import com.skyinit.pomodorotimer.ui.profile.AppCategoryEditViewModel;
import com.skyinit.pomodorotimer.ui.profile.ProfileViewModel;
import com.skyinit.pomodorotimer.ui.profile.devlab.DevLabViewModel;
import com.skyinit.pomodorotimer.ui.settings.PomodoroSettingsViewModel;
import com.skyinit.pomodorotimer.ui.settings.SettingsHubViewModel;
import com.skyinit.pomodorotimer.ui.settings.SystemPermissionsViewModel;
import com.skyinit.pomodorotimer.ui.settings.ThemeColorViewModel;
import com.skyinit.pomodorotimer.ui.statistics.StatisticsViewModel;

/**
 * 统一 ViewModel 工厂，从 {@link com.skyinit.pomodorotimer.AppContainer} 注入各 Repository 依赖。
 */
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
        if (modelClass.isAssignableFrom(HomeTodoViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new HomeTodoViewModel(
                    application,
                    container.getTodoWorkspaceRepository(),
                    container.getUserSessionRepository()
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
                    container.getUserAppBlockingRepository(),
                    container.getStatisticsRepository()
            );
        }
        if (modelClass.isAssignableFrom(DevLabViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new DevLabViewModel(
                    application,
                    container.getDevLabRepository()
            );
        }
        if (modelClass.isAssignableFrom(SettingsHubViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new SettingsHubViewModel(
                    application,
                    container.getSettingsManager(),
                    container.getUserPomodoroSettingsRepository());
        }
        if (modelClass.isAssignableFrom(ThemeColorViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new ThemeColorViewModel(
                    application,
                    container.getSettingsManager());
        }
        if (modelClass.isAssignableFrom(PomodoroSettingsViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new PomodoroSettingsViewModel(
                    application,
                    container.getUserPomodoroSettingsRepository(),
                    container.getSettingsManager());
        }
        if (modelClass.isAssignableFrom(SystemPermissionsViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new SystemPermissionsViewModel(application);
        }
        if (modelClass.isAssignableFrom(SessionDetailViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new SessionDetailViewModel(
                    application,
                    container.getSessionRepository(),
                    container.getStatisticsRepository(),
                    container.getUserSessionRepository()
            );
        }
        if (modelClass.isAssignableFrom(SessionBlockRecordsViewModel.class)) {
            return (T) new SessionBlockRecordsViewModel(
                    container.getSessionBlockRecordRepository(),
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
        if (modelClass.isAssignableFrom(AccountSecurityViewModel.class)) {
            return (T) new AccountSecurityViewModel(
                    container.getUserSessionRepository(),
                    container.getAccountOperationGuard());
        }
        if (modelClass.isAssignableFrom(EditNicknameViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new EditNicknameViewModel(
                    application,
                    container.getUserSessionRepository());
        }
        if (modelClass.isAssignableFrom(EditSignatureViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new EditSignatureViewModel(
                    application,
                    container.getUserSessionRepository());
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
        if (modelClass.isAssignableFrom(FirstRegisterViewModel.class)) {
            Application application = (Application) container.getAppContext();
            return (T) new FirstRegisterViewModel(application, container.getUserSessionRepository());
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

    public ViewModelProvider.Factory createAppBlockingFactory(String userId) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(AppBlockingViewModel.class)) {
                    return (T) new AppBlockingViewModel(
                            container.getBlockedAppRepository(),
                            userId
                    );
                }
                throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
            }
        };
    }

    public ViewModelProvider.Factory createAppCategoryEditFactory(String userId, String packageName) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(AppCategoryEditViewModel.class)) {
                    return (T) new AppCategoryEditViewModel(
                            container.getBlockedAppRepository(),
                            userId,
                            packageName
                    );
                }
                throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
            }
        };
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
                            container.getTodoWorkspaceRepository(),
                            taskId,
                            taskType
                    );
                }
                throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
            }
        };
    }
}
