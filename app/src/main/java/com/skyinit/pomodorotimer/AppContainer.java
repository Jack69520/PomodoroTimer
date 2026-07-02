package com.skyinit.pomodorotimer;

import android.content.Context;

import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.AccountOperationGuard;
import com.skyinit.pomodorotimer.data.repository.DataBackupRepository;
import com.skyinit.pomodorotimer.data.repository.DataBackupRepositoryImpl;
import com.skyinit.pomodorotimer.data.repository.RecurringTaskManager;
import com.skyinit.pomodorotimer.data.repository.SessionRepository;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerStateRepository;
import com.skyinit.pomodorotimer.data.repository.TodoRepository;
import com.skyinit.pomodorotimer.data.repository.TaskRepository;
import com.skyinit.pomodorotimer.data.repository.TodoFilterManager;
import com.skyinit.pomodorotimer.data.repository.UserPomodoroSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.UserSessionRepository;
import com.skyinit.pomodorotimer.ui.ViewModelFactory;

/**
 * 应用级依赖容器，集中提供 Repository 与 ViewModelFactory。
 */
public final class AppContainer {

    private static volatile AppContainer instance;

    private final Context appContext;
    private final AppDatabase database;
    private final SettingsManager settingsManager;
    private final AccountManager accountManager;
    private final UserSessionRepository userSessionRepository;
    private final UserPomodoroSettingsRepository userPomodoroSettingsRepository;
    private final TimerSettingsRepository timerSettingsRepository;
    private final TimerStateRepository timerStateRepository;
    private final AccountOperationGuard accountOperationGuard;
    private final StatisticsRepository statisticsRepository;
    private final SessionRepository sessionRepository;
    private final TodoFilterManager todoFilterManager;
    private final TodoRepository todoRepository;
    private final TaskRepository taskRepository;
    private final RecurringTaskManager recurringTaskManager;
    private final DataBackupRepository dataBackupRepository;
    private final ViewModelFactory viewModelFactory;

    public AppContainer(Context context) {
        appContext = context.getApplicationContext();
        database = AppDatabase.getDatabase(appContext);
        settingsManager = new SettingsManager(appContext);
        accountManager = AccountManager.getInstance(appContext);
        userPomodoroSettingsRepository = new UserPomodoroSettingsRepository(database, accountManager);
        userSessionRepository = new UserSessionRepository(accountManager, userPomodoroSettingsRepository);
        timerSettingsRepository = new TimerSettingsRepository(userPomodoroSettingsRepository);
        timerStateRepository = new TimerStateRepository(timerSettingsRepository);
        accountOperationGuard = new AccountOperationGuard(appContext, settingsManager, timerStateRepository);
        statisticsRepository = new StatisticsRepository(appContext, accountManager);
        sessionRepository = new SessionRepository(database.pomodoroSessionDao(), accountManager);
        todoFilterManager = new TodoFilterManager(appContext);
        todoRepository = new TodoRepository(
                database.todoDao(),
                accountManager,
                todoFilterManager
        );
        taskRepository = new TaskRepository(appContext, accountManager);
        recurringTaskManager = new RecurringTaskManager(appContext);
        dataBackupRepository = new DataBackupRepositoryImpl(appContext);
        viewModelFactory = new ViewModelFactory(this);
    }

    public static AppContainer getInstance(Context context) {
        if (instance == null) {
            synchronized (AppContainer.class) {
                if (instance == null) {
                    instance = new AppContainer(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public static void init(Context context) {
        getInstance(context);
    }

    public Context getAppContext() {
        return appContext;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public UserSessionRepository getUserSessionRepository() {
        return userSessionRepository;
    }

    public UserPomodoroSettingsRepository getUserPomodoroSettingsRepository() {
        return userPomodoroSettingsRepository;
    }

    public TimerSettingsRepository getTimerSettingsRepository() {
        return timerSettingsRepository;
    }

    public TimerStateRepository getTimerStateRepository() {
        return timerStateRepository;
    }

    public AccountOperationGuard getAccountOperationGuard() {
        return accountOperationGuard;
    }

    public StatisticsRepository getStatisticsRepository() {
        return statisticsRepository;
    }

    public SessionRepository getSessionRepository() {
        return sessionRepository;
    }

    public TodoFilterManager getTodoFilterManager() {
        return todoFilterManager;
    }

    public TodoRepository getTodoRepository() {
        return todoRepository;
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public RecurringTaskManager getRecurringTaskManager() {
        return recurringTaskManager;
    }

    public DataBackupRepository getDataBackupRepository() {
        return dataBackupRepository;
    }

    public ViewModelFactory getViewModelFactory() {
        return viewModelFactory;
    }
}
