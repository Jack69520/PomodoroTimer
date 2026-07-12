package com.skyinit.pomodorotimer;

import androidx.room.TypeConverter;
import java.util.Arrays;
import java.util.List;

public class Converters {
    @TypeConverter
    public static String fromStringList(List<String> value) {
        if (value == null) return null;
        return String.join(",", value);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null) return null;
        return Arrays.asList(value.split(","));
    }
}
