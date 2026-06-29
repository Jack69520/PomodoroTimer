package com.skyinit.pomodorotimer.data.database;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseMigrationsTest {

    @Test
    public void currentVersion_isOne() {
        assertEquals(1, DatabaseMigrations.CURRENT_VERSION);
    }

    @Test
    public void allMigrations_isNonNullArray() {
        assertNotNull(DatabaseMigrations.ALL);
    }

    @Test
    public void exportedSchema_v1_exists() throws Exception {
        Path schemaPath = Paths.get("schemas", "com.skyinit.pomodorotimer.AppDatabase", "1.json");
        File schemaFile = schemaPath.toFile();
        assertTrue("Schema export missing: " + schemaPath, schemaFile.exists());
        String json = new String(Files.readAllBytes(schemaFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"user_pomodoro_settings\""));
        assertTrue(json.contains("defaultStudyTimeMs"));
        assertTrue(json.contains("dndDuringFocusEnabled"));
    }
}
