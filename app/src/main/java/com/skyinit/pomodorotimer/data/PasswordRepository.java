package com.skyinit.pomodorotimer.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.UserDao;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.security.PasswordHasher;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

public class PasswordRepository {

    private static final String TAG = "PasswordRepository";
    private static final String PREFS_NAME = "password_security_prefs";
    private static final String KEY_SALT_PREFIX = "salt_";

    private static final PasswordRepository INSTANCE = new PasswordRepository();

    private final AppExecutors appExecutors = AppExecutors.getInstance();

    private PasswordRepository() {
    }

    public static PasswordRepository getInstance() {
        return INSTANCE;
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private UserDao getUserDao(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        return db.userDao();
    }

    public void hashAndStorePassword(Context context, User user, String rawPassword) {
        String hash = PasswordHasher.hashPassword(rawPassword);
        user.password = hash;
        user.passwordSalt = PasswordHasher.extractSaltHex(hash);
        if (user.passwordSalt != null) {
            // commit：密码相关盐必须与内存哈希同边界落盘，避免进程被杀后盐/哈希分叉
            getPrefs(context).edit()
                    .putString(KEY_SALT_PREFIX + user.userId, user.passwordSalt)
                    .commit();
        }
    }

    public void clearStoredSalt(Context context, String userId) {
        getPrefs(context).edit().remove(KEY_SALT_PREFIX + userId).apply();
    }

    public boolean verifyPassword(@Nullable String hash, @Nullable String password) {
        return PasswordHasher.verifyPassword(password, hash);
    }

    /**
     * 更新内存哈希并写入 Room。已在 diskIo 线程时同步执行，避免嵌套 enqueue
     * 导致「成功回调早于 DB 落库」的竞态。
     */
    public void updatePassword(Context context, User user, String newPassword) {
        if (appExecutors.isDiskIoThread()) {
            updatePasswordOnDisk(context, user, newPassword);
            return;
        }
        appExecutors.diskIo(() -> updatePasswordOnDisk(context, user, newPassword),
                throwable -> AppLog.e(TAG, "Failed to update password", throwable));
    }

    /** 须在 diskIo 线程调用：哈希写入内存/Prefs 后立刻同步更新 Room。 */
    public void updatePasswordOnDisk(Context context, User user, String newPassword) {
        hashAndStorePassword(context, user, newPassword);
        getUserDao(context).updatePassword(user.userId, user.password, user.passwordSalt);
    }
}
