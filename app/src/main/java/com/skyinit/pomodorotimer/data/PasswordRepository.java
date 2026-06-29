package com.skyinit.pomodorotimer.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.dao.UserDao;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.security.PasswordHasher;
import com.skyinit.pomodorotimer.util.AppExecutors;


public class PasswordRepository {

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

            getPrefs(context).edit().putString(KEY_SALT_PREFIX + user.userId, user.passwordSalt).apply();

        }

    }



    @Nullable

    public String getStoredSalt(Context context, String userId) {

        return getPrefs(context).getString(KEY_SALT_PREFIX + userId, null);

    }



    public boolean verifyPassword(@Nullable String salt, @Nullable String hash, @Nullable String password) {

        return PasswordHasher.verifyPassword(password, hash, salt);

    }



    public boolean needsUpgrade(@Nullable String storedHash) {

        return PasswordHasher.isLegacyHash(storedHash);

    }



    /** 登录成功后把旧 SHA-256 哈希升级为 PBKDF2。 */

    public void upgradeToPbkdf2(Context context, User user, String rawPassword) {

        hashAndStorePassword(context, user, rawPassword);

        appExecutors.diskIo(() -> getUserDao(context).updatePassword(

                user.userId, user.password, user.passwordSalt));

    }



    public void updatePassword(Context context, User user, String newPassword) {

        hashAndStorePassword(context, user, newPassword);

        appExecutors.diskIo(() -> getUserDao(context).updatePassword(

                user.userId, user.password, user.passwordSalt));

    }

}

