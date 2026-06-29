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
import com.skyinit.pomodorotimer.data.dao.UserPomodoroSettingsDao;
import com.skyinit.pomodorotimer.data.database.DatabaseMigrations;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.entity.PomodoroSession;
import com.skyinit.pomodorotimer.data.entity.RecurringTask;
import com.skyinit.pomodorotimer.data.entity.SubTask;
import com.skyinit.pomodorotimer.data.entity.TaskCategory;
import com.skyinit.pomodorotimer.data.entity.TodoItem;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.data.entity.UserPomodoroSettings;

@Database(entities = {
        TodoItem.class,
        PomodoroSession.class,
        TaskCategory.class,
        SubTask.class,
        RecurringTask.class,
        BlockedApp.class,
        User.class,
        UserPomodoroSettings.class
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

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "pomodoro_db")
                            .addMigrations(DatabaseMigrations.ALL)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
