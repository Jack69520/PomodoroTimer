# Room：保留 Database、Dao、Entity 及注解处理器生成的 *_Impl（Release 反射实例化依赖无参构造）
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep @androidx.room.Database class *
-keep @androidx.room.Dao interface *
-keep @androidx.room.Entity class *
-keep class **_Impl {
    <init>(...);
}
-keep class **.*_Impl {
    <init>(...);
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.skyinit.pomodorotimer.Converters { *; }
-keep class com.skyinit.pomodorotimer.AppDatabase { *; }
-dontwarn androidx.room.paging.**

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# MPAndroidChart
-dontwarn com.github.mikephil.charting.**
-keep class com.github.mikephil.charting.** { *; }

# 保留行号便于崩溃分析
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ViewModel / LiveData
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Navigation 通过 XML 中的类名反射实例化 Fragment，Release 混淆时必须保留实际类名
-keep class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# 枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
