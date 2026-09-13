package com.skyinit.pomodorotimer;

import android.content.Context;

import com.skyinit.pomodorotimer.data.repository.AccountManager;
import com.skyinit.pomodorotimer.data.repository.AccountOperationGuard;
import com.skyinit.pomodorotimer.data.repository.BlockedAppRepository;
import com.skyinit.pomodorotimer.data.repository.DataBackupRepository;
import com.skyinit.pomodorotimer.data.repository.DataBackupRepositoryImpl;
import com.skyinit.pomodorotimer.data.repository.DevLabRepository;
import com.skyinit.pomodorotimer.data.repository.SessionBlockRecordRepository;
import com.skyinit.pomodorotimer.data.repository.SessionRepository;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.data.repository.StatisticsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerSettingsRepository;
import com.skyinit.pomodorotimer.data.repository.TimerStateRepository;
import com.skyinit.pomodorotimer.data.repository.TodoWorkspaceRepository;
import com.skyinit.pomodorotimer.data.repository.UserAppBlockingRepository;
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
    private final UserAppBlockingRepository userAppBlockingRepository;
    private final TimerSettingsRepository timerSettingsRepository;
    private final TimerStateRepository timerStateRepository;
    private final AccountOperationGuard accountOperationGuard;
    private final StatisticsRepository statisticsRepository;
    private final SessionRepository sessionRepository;
    private final TodoWorkspaceRepository todoWorkspaceRepository;
    private final DataBackupRepository dataBackupRepository;
    private final BlockedAppRepository blockedAppRepository;
    private final SessionBlockRecordRepository sessionBlockRecordRepository;
    private final DevLabRepository devLabRepository;
    private final ViewModelFactory viewModelFactory;

    public AppContainer(Context context) {
        appContext = context.getApplicationContext();
        database = AppDatabase.getDatabase(appContext);
        settingsManager = new SettingsManager(appContext);
        accountManager = AccountManager.getInstance(appContext);
        userPomodoroSettingsRepository = new UserPomodoroSettingsRepository(settingsManager);
        userAppBlockingRepository = new UserAppBlockingRepository(database, accountManager);
        timerSettingsRepository = new TimerSettingsRepository(userPomodoroSettingsRepository);
        timerStateRepository = new TimerStateRepository(timerSettingsRepository);
        accountManager.setTimerStateRepository(timerStateRepository);
        accountOperationGuard = new AccountOperationGuard(
                appContext, userAppBlockingRepository, timerStateRepository);
        userSessionRepository = new UserSessionRepository(
                accountManager,
                userPomodoroSettingsRepository,
                userAppBlockingRepository,
                accountOperationGuard);
        statisticsRepository = new StatisticsRepository(appContext, accountManager);
        sessionRepository = new SessionRepository(database.pomodoroSessionDao(), accountManager);
        todoWorkspaceRepository = new TodoWorkspaceRepository(appContext, accountManager);
        dataBackupRepository = new DataBackupRepositoryImpl(appContext);
        blockedAppRepository = new BlockedAppRepository(
                appContext,
                database.blockedAppDao(),
                accountManager
        );
        sessionBlockRecordRepository = new SessionBlockRecordRepository(
                database.sessionAppBlockRecordDao()
        );
        devLabRepository = new DevLabRepository(appContext);
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

    public static void resetForTest() {
        synchronized (AppContainer.class) {
            instance = null;
        }
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

    public UserAppBlockingRepository getUserAppBlockingRepository() {
        return userAppBlockingRepository;
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

    public TodoWorkspaceRepository getTodoWorkspaceRepository() {
        return todoWorkspaceRepository;
    }

    public DataBackupRepository getDataBackupRepository() {
        return dataBackupRepository;
    }

    public BlockedAppRepository getBlockedAppRepository() {
        return blockedAppRepository;
    }

    public SessionBlockRecordRepository getSessionBlockRecordRepository() {
        return sessionBlockRecordRepository;
    }

    public DevLabRepository getDevLabRepository() {
        return devLabRepository;
    }

    public ViewModelFactory getViewModelFactory() {
        return viewModelFactory;
    }
}
