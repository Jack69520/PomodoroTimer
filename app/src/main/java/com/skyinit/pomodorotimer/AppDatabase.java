package com.skyinit.pomodorotimer;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.skyinit.pomodorotimer.data.dao.BlockedAppDao;
import com.skyinit.pomodorotimer.data.dao.PomodoroSessionDao;
import com.skyinit.pomodorotimer.data.dao.RecurringTaskDao;
import com.skyinit.pomodorotimer.data.dao.SubTaskDao;
import com.skyinit.pomodorotimer.data.dao.TaskCategoryDao;
import com.skyinit.pomodorotimer.data.dao.TodoDao;
import com.skyinit.pomodorotimer.data.dao.UserDao;
import com.skyinit.pomodorotimer.data.dao.SessionAppBlockRecordDao;
import com.skyinit.pomodorotimer.data.dao.UserPomodoroSettingsDao;
import com.skyinit.pomodorotimer.data.database.DatabaseMigrations;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.entity.RecurringTask;
import com.skyinit.pomodorotimer.data.entity.SessionAppBlockRecord;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TaskCategory;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;

/**
 * Room 本地数据库，存储待办、番茄钟记录、屏蔽应用列表、用户档案等全部业务数据。
 * 单例懒加载，通过 {@link com.skyinit.pomodorotimer.data.database.DatabaseMigrations} 管理版本迁移。
 */
@Database(entities = {
        TodoItem.class,
        PomodoroSession.class,
        TaskCategory.class,
        SubTask.class,
        RecurringTask.class,
        BlockedApp.class,
        User.class,
        UserPomodoroSettings.class,
        SessionAppBlockRecord.class
}, version = DatabaseMigrations.CURRENT_VERSION, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract TodoDao todoDao();
    public abstract PomodoroSessionDao pomodoroSessionDao();
    public abstract TaskCategoryDao taskCategoryDao();
    public abstract SubTaskDao subTaskDao();
    public abstract RecurringTaskDao recurringTaskDao();
    public abstract BlockedAppDao blockedAppDao();
    public abstract UserDao userDao();
    public abstract UserPomodoroSettingsDao userPomodoroSettingsDao();
    public abstract SessionAppBlockRecordDao sessionAppBlockRecordDao();

    /** 线程安全的单例获取，首次调用时创建数据库实例。 */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    RoomDatabase.Builder<AppDatabase> builder;
                    if (isRobolectricEnvironment()) {
                        builder = Room.inMemoryDatabaseBuilder(appContext, AppDatabase.class)
                                .allowMainThreadQueries();
                    } else {
                        builder = Room.databaseBuilder(appContext, AppDatabase.class, "pomodoro_db")
                                .addMigrations(DatabaseMigrations.ALL);
                    }
                    INSTANCE = builder.build();
                }
            }
        }
        return INSTANCE;
    }

    private static boolean isRobolectricEnvironment() {
        try {
            Class.forName("org.robolectric.RuntimeEnvironment");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static void resetForTest() {
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }
}
