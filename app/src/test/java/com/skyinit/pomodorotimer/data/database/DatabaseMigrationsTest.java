package com.skyinit.pomodorotimer.data.database;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DatabaseMigrationsTest {

    @Test
    public void currentVersion_isTwo() {
        assertEquals(2, DatabaseMigrations.CURRENT_VERSION);
    }

    @Test
    public void allMigrations_contains1to2() {
        assertNotNull(DatabaseMigrations.ALL);
        assertEquals(1, DatabaseMigrations.ALL.length);
        assertEquals(1, DatabaseMigrations.MIGRATION_1_2.startVersion);
        assertEquals(2, DatabaseMigrations.MIGRATION_1_2.endVersion);
    }

    @Test
    public void exportedSchema_v1_and_v2_exist() throws Exception {
        Path v1 = Paths.get("schemas", "com.skyinit.pomodorotimer.AppDatabase", "1.json");
        Path v2 = Paths.get("schemas", "com.skyinit.pomodorotimer.AppDatabase", "2.json");
        assertTrue("Schema export missing: " + v1, v1.toFile().exists());
        assertTrue("Schema export missing: " + v2, v2.toFile().exists());
        String json1 = new String(Files.readAllBytes(v1), StandardCharsets.UTF_8);
        String json2 = new String(Files.readAllBytes(v2), StandardCharsets.UTF_8);
        assertTrue(json1.contains("pomodoro_sessions"));
        assertTrue(json2.contains("pausedDurationMs"));
        assertTrue(json2.contains("plannedDurationMs"));
        assertFalse("v1 must not already declare pausedDurationMs",
                json1.contains("pausedDurationMs"));
    }

    @Test
    public void migrate1to2_preservesDuration_andDefaultsNewColumns() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        SupportSQLiteOpenHelper.Configuration config =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name("migration_manual_test.db")
                        .callback(new SupportSQLiteOpenHelper.Callback(1) {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL(
                                        "CREATE TABLE pomodoro_sessions ("
                                                + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                                                + "userId TEXT, "
                                                + "startTime INTEGER NOT NULL, "
                                                + "endTime INTEGER NOT NULL, "
                                                + "duration INTEGER NOT NULL, "
                                                + "completed INTEGER NOT NULL, "
                                                + "pauseReason TEXT, "
                                                + "pauseCount INTEGER NOT NULL, "
                                                + "pauseReasons TEXT, "
                                                + "earlyEnd INTEGER NOT NULL, "
                                                + "notes TEXT, "
                                                + "taskId INTEGER NOT NULL, "
                                                + "subTaskId INTEGER NOT NULL, "
                                                + "category TEXT, "
                                                + "tags TEXT, "
                                                + "blockEventCount INTEGER NOT NULL)");
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                            }
                        })
                        .build();
        SupportSQLiteOpenHelper helper =
                new FrameworkSQLiteOpenHelperFactory().create(config);
        SupportSQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(
                "INSERT INTO pomodoro_sessions "
                        + "(userId, startTime, endTime, duration, completed, pauseCount, "
                        + "earlyEnd, taskId, subTaskId, blockEventCount) "
                        + "VALUES ('u1', 1000, 2000, 1500000, 1, 1, 0, -1, -1, 0)");

        DatabaseMigrations.MIGRATION_1_2.migrate(db);

        Cursor c = db.query(
                "SELECT duration, pausedDurationMs, plannedDurationMs, pauseCount "
                        + "FROM pomodoro_sessions WHERE userId='u1'");
        assertTrue(c.moveToFirst());
        assertEquals(1_500_000L, c.getLong(0));
        assertEquals(0L, c.getLong(1));
        assertEquals(0L, c.getLong(2));
        assertEquals(1, c.getInt(3));
        c.close();
        db.close();
    }
}
