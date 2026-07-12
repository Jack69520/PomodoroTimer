package com.skyinit.pomodorotimer.data.repository;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.data.PasswordRepository;
import com.skyinit.pomodorotimer.data.dao.UserDao;
import com.skyinit.pomodorotimer.data.entity.User;

import androidx.annotation.NonNull;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.util.AppLog;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.skyinit.pomodorotimer.util.AppExecutors;

/**
 * 管理当前活跃档案（本地或注册）及账户切换。
 * <p>
 * 应用启动后始终存在一个有效 userId；本地档案免密可用，注册账户需密码登录/切换。
 */
public class AccountManager {
    private static final String PREFS_NAME = "account_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_HAS_ACTIVE_PROFILE = "has_active_profile";
    private static final String LOCAL_USER_ID_PREFIX = "L";

    private static AccountManager instance;
    private final Context context;
    private final AppDatabase database;
    private final UserDao userDao;
    private final SharedPreferences prefs;
    private final PasswordRepository passwordRepository;
    private final AccountTimerStateGuard timerStateGuard;
    private final AppExecutors appExecutors = AppExecutors.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile User currentUser;
    private ActiveUserListener activeUserListener;

    public interface ActiveUserListener {
        void onActiveUserChanged(@NonNull User user, boolean sessionSwitched);
    }

    public void setActiveUserListener(ActiveUserListener listener) {
        this.activeUserListener = listener;
    }

    private AccountManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getDatabase(this.context);
        this.userDao = database.userDao();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.passwordRepository = PasswordRepository.getInstance();
        this.timerStateGuard = new AccountTimerStateGuard(this.context);
    }

    public static synchronized AccountManager getInstance(Context context) {
        if (instance == null) {
            instance = new AccountManager(context);
        }
        return instance;
    }

    public static synchronized void resetForTest() {
        instance = null;
    }

    /** 注入计时态仓库，使账户切换守卫与 UI 层判定一致。 */
    public void setTimerStateRepository(TimerStateRepository timerStateRepository) {
        timerStateGuard.setTimerStateRepository(timerStateRepository);
    }

    /**
     * 冷启动时确保存在活跃档案（异步，主线程安全）。
     * 结果通过回调在主线程通知。
     */
    public void ensureDefaultProfileAsync(@NonNull Runnable onSuccess, @NonNull Consumer<Throwable> onError) {
        if (currentUser != null) {
            mainHandler.post(onSuccess);
            return;
        }
        appExecutors.diskIo(() -> {
            try {
                ensureDefaultProfileOnDisk();
                mainHandler.post(onSuccess);
            } catch (Throwable throwable) {
                AppLog.e("AccountManager", "ensureDefaultProfileAsync failed", throwable);
                mainHandler.post(() -> onError.accept(throwable));
            }
        }, throwable -> mainHandler.post(() -> onError.accept(throwable)));
    }

    /**
     * 后台线程专用：同步确保活跃档案（供 WorkManager 等后台任务调用，禁止在主线程使用）。
     */
    public void ensureDefaultProfileBlocking() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("ensureDefaultProfileBlocking must not run on the main thread");
        }
        if (currentUser != null) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        appExecutors.diskIo(() -> {
            try {
                ensureDefaultProfileOnDisk();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        }, throwable -> {
            failure.compareAndSet(null, throwable);
            latch.countDown();
        });
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("ensureDefaultProfile timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ensureDefaultProfile interrupted", e);
        }
        Throwable throwable = failure.get();
        if (throwable != null) {
            throw new IllegalStateException("ensureDefaultProfile failed", throwable);
        }
    }

    /**
     * 由 {@link com.skyinit.pomodorotimer.App} 在磁盘 IO 线程调用。
     */
    public void ensureDefaultProfileOnDisk() {
        if (currentUser != null) {
            return;
        }
        String userId = prefs.getString(KEY_CURRENT_USER_ID, null);
        if (userId != null) {
            User user = userDao.getUserById(userId);
            if (user != null) {
                activateUserOnDisk(user);
                return;
            }
        }
        User local = createLocalUser();
        userDao.insert(local);
        activateUserOnDisk(local);
    }

    /** 当前活跃档案 ID，永不为 null（需先完成应用初始化）。 */
    @NonNull
    public String requireActiveUserId() {
        User user = requireCurrentUser();
        return user.userId;
    }

    /** 当前活跃档案，永不为 null。 */
    @NonNull
    public User requireCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No active profile; wait for app initialization");
        }
        return currentUser;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserId() {
        return currentUser != null ? currentUser.userId : null;
    }

    public boolean hasActiveProfile() {
        return currentUser != null;
    }

    public boolean isLocalProfile() {
        return currentUser != null && currentUser.isLocalProfile();
    }

    /** 当前活跃档案是否为已注册账户（含密码）。 */
    public boolean isRegistered() {
        return currentUser != null && currentUser.isRegistered();
    }

    /**
     * @deprecated 请使用 {@link #isRegistered()} 或 {@link #hasActiveProfile()}
     */
    @Deprecated
    public boolean isLoggedIn() {
        return isRegistered();
    }

    /**
     * 凭证登录。IO 在 {@code diskIo} 线程执行；{@link LoginCallback} 一律在主线程回调，
     * 以便 ViewModel 安全使用 {@code LiveData.setValue} 驱动 UI。
     */
    public void login(String userId, String password, LoginCallback callback) {
        appExecutors.diskIo(() -> {
            try {
                if (hasActiveTimerSession()) {
                    dispatchLoginError(callback, getActiveTimerSessionMessage());
                    return;
                }
                User user = userDao.getUserById(userId);
                if (user == null || !user.isRegistered()) {
                    dispatchLoginError(callback,
                            context.getString(R.string.account_error_login_invalid));
                    return;
                }
                if (!passwordRepository.verifyPassword(user.password, password)) {
                    dispatchLoginError(callback,
                            context.getString(R.string.account_error_login_invalid));
                    return;
                }
                userDao.updateLastLoginTime(userId, System.currentTimeMillis());
                final User loggedInUser = userDao.getUserById(userId);
                activateUserOnDisk(loggedInUser);
                dispatchLoginSuccess(callback, loggedInUser);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Login error", e);
                dispatchLoginError(callback,
                        context.getString(R.string.account_error_login_failed, e.getMessage()));
            }
        });
    }

    /** 将登录结果派发到主线程，保证 ViewModel / UI 层线程契约一致。 */
    private void dispatchLoginSuccess(LoginCallback callback, User user) {
        mainHandler.post(() -> callback.onSuccess(user));
    }

    private void dispatchLoginError(LoginCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    public void recoverLogin(String userId, String nickname, LoginCallback callback) {
        appExecutors.diskIo(() -> {
            try {
                if (hasActiveTimerSession()) {
                    mainHandler.post(() -> callback.onError(getActiveTimerSessionMessage()));
                    return;
                }
                User user = userDao.getUserByIdAndNickname(userId, nickname);
                if (user == null || !user.isRegistered()) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_recovery_mismatch)));
                    return;
                }
                forcePasswordReset = true;
                userDao.updateLastLoginTime(userId, System.currentTimeMillis());
                final User loggedInUser = userDao.getUserById(userId);
                activateUserOnDisk(loggedInUser);
                mainHandler.post(() -> callback.onSuccess(loggedInUser));
            } catch (Exception e) {
                AppLog.e("AccountManager", "Recover login error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_recovery_failed, e.getMessage())));
            }
        });
    }

    private boolean forcePasswordReset = false;

    public boolean isForcePasswordReset() {
        return forcePasswordReset;
    }

    public void clearForcePasswordReset() {
        forcePasswordReset = false;
    }

    /**
     * 本地档案升级为注册账户（沿用当前 userId，数据不迁移）。
     */
    public void upgradeLocalProfile(String nickname, String password, String confirmPassword,
                                    String avatarPath, String signature, RegisterCallback callback) {
        appExecutors.diskIo(() -> {
            try {
                if (currentUser == null || !currentUser.isLocalProfile()) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_upgrade_not_local)));
                    return;
                }
                if (!isValidPassword(password)) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_password_invalid)));
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_password_confirm_mismatch)));
                    return;
                }
                User user = userDao.getUserById(currentUser.userId);
                if (user == null) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_profile_not_found)));
                    return;
                }
                if (nickname != null && !nickname.isEmpty()) {
                    user.nickname = nickname;
                }
                if (avatarPath != null) {
                    user.avatarPath = avatarPath;
                }
                if (signature != null) {
                    user.signature = signature;
                }
                passwordRepository.hashAndStorePassword(context, user, password);
                user.accountType = User.ACCOUNT_TYPE_REGISTERED;
                userDao.update(user);
                currentUser = userDao.getUserById(user.userId);
                notifyActiveUserChanged(false);
                mainHandler.post(() -> callback.onSuccess(currentUser));
            } catch (Exception e) {
                AppLog.e("AccountManager", "Upgrade profile error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_upgrade_failed, e.getMessage())));
            }
        });
    }

    /** 注册：本地档案原地升级；已是注册账户则拒绝。 */
    public void register(String nickname, String password, String confirmPassword,
                         String avatarPath, String signature, RegisterCallback callback) {
        if (isLocalProfile()) {
            upgradeLocalProfile(nickname, password, confirmPassword, avatarPath, signature, callback);
            return;
        }
        if (isRegistered()) {
            callback.onError(context.getString(R.string.account_error_already_registered));
            return;
        }
        callback.onError(context.getString(R.string.account_error_profile_not_initialized));
    }

    /**
     * 注册账户登出：新建本地档案并切换，原注册账户数据保留在 Room。
     */
    public void logout(LogoutCallback callback) {
        appExecutors.diskIo(() -> {
            try {
                if (!isRegistered()) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_local_no_logout)));
                    return;
                }
                if (hasActiveTimerSession()) {
                    mainHandler.post(() -> callback.onError(getActiveTimerSessionMessage()));
                    return;
                }
                User newLocal = createLocalUser();
                userDao.insert(newLocal);
                activateUserOnDisk(newLocal);
                forcePasswordReset = false;
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Logout error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_logout_failed, e.getMessage())));
            }
        });
    }

    public void deleteCurrentAccount(AccountDeletionCallback callback) {
        if (!isRegistered() || currentUser == null) {
            callback.onError(context.getString(R.string.account_error_delete_registered_only));
            return;
        }
        if (hasActiveTimerSession()) {
            callback.onError(getActiveTimerSessionMessage());
            return;
        }

        String userId = currentUser.userId;
        appExecutors.diskIo(() -> {
            try {
                database.runInTransaction(() -> {
                    database.subTaskDao().deleteSubtasksByUserId(userId);
                    database.todoDao().deleteTodosByUserId(userId);
                    database.pomodoroSessionDao().deleteSessionsByUserId(userId);
                    database.userPomodoroSettingsDao().deleteByUserId(userId);
                    database.blockedAppDao().deleteByUserId(userId);
                    database.recurringTaskDao().deleteByUserId(userId);
                    userDao.deleteUserById(userId);
                });

                if (userDao.getUserById(userId) != null) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_delete_failed_retry)));
                    return;
                }

                passwordRepository.clearStoredSalt(context, userId);

                User newLocal = createLocalUser();
                userDao.insert(newLocal);
                activateUserOnDisk(newLocal);
                forcePasswordReset = false;
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Delete account error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_delete_failed, e.getMessage())));
            }
        });
    }

    public void updatePassword(String oldPassword, String newPassword, String confirmPassword,
                              PasswordUpdateCallback callback) {
        if (!isRegistered() || currentUser == null) {
            callback.onError(context.getString(R.string.account_error_login_required));
            return;
        }
        appExecutors.diskIo(() -> {
            try {
                if (!forcePasswordReset && !passwordRepository.verifyPassword(
                        currentUser.password, oldPassword)) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_wrong_old_password)));
                    return;
                }
                String invalidReason = getPasswordInvalidReason(newPassword);
                if (invalidReason != null) {
                    mainHandler.post(() -> callback.onError(context.getString(
                            R.string.account_error_new_password_invalid, invalidReason)));
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_password_mismatch)));
                    return;
                }
                passwordRepository.updatePassword(context, currentUser, newPassword);
                forcePasswordReset = false;
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Update password error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_password_update_failed, e.getMessage())));
            }
        });
    }

    /** 本地与注册账户均可修改资料。 */
    public void updateProfile(String nickname, String avatarPath, String signature,
                              ProfileUpdateCallback callback) {
        if (currentUser == null) {
            callback.onError(context.getString(R.string.account_error_profile_not_ready));
            return;
        }
        appExecutors.diskIo(() -> {
            try {
                String userId = currentUser.userId;
                if (nickname != null && !nickname.isEmpty()) {
                    userDao.updateNickname(userId, nickname);
                    currentUser.nickname = nickname;
                }
                if (avatarPath != null) {
                    userDao.updateAvatar(userId, avatarPath);
                    currentUser.avatarPath = avatarPath;
                }
                if (signature != null) {
                    userDao.updateSignature(userId, signature);
                    currentUser.signature = signature;
                }
                notifyActiveUserChanged(false);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Update profile error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_profile_update_failed, e.getMessage())));
            }
        });
    }

    private User createLocalUser() {
        User user = new User();
        user.userId = generateLocalUserId();
        user.nickname = context.getString(R.string.account_default_nickname);
        user.password = "";
        user.passwordSalt = "";
        user.signature = "";
        user.accountType = User.ACCOUNT_TYPE_LOCAL;
        user.createdAt = System.currentTimeMillis();
        user.lastLoginAt = System.currentTimeMillis();
        return user;
    }

    private String generateLocalUserId() {
        String userId;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
            userId = LOCAL_USER_ID_PREFIX + suffix;
        } while (userDao.checkUserIdExists(userId) > 0);
        return userId;
    }

    private boolean hasActiveTimerSession() {
        return timerStateGuard.hasActiveTimerState();
    }

    private String getActiveTimerSessionMessage() {
        return timerStateGuard.getBlockedMessage();
    }

    private void activateUserOnDisk(User user) {
        currentUser = user;
        prefs.edit()
                .putBoolean(KEY_HAS_ACTIVE_PROFILE, true)
                .putString(KEY_CURRENT_USER_ID, user.userId)
                .commit();
        notifyActiveUserChanged(true);
    }

    private void notifyActiveUserChanged(boolean sessionSwitched) {
        User user = currentUser;
        if (user == null || activeUserListener == null) {
            return;
        }
        mainHandler.post(() -> activeUserListener.onActiveUserChanged(user, sessionSwitched));
    }

    private boolean isValidPassword(String password) {
        return getPasswordInvalidReason(password) == null;
    }

    private String getPasswordInvalidReason(String password) {
        if (password == null) return context.getString(R.string.account_password_empty);
        if (password.length() < 6 || password.length() > 16) {
            return context.getString(R.string.account_password_length);
        }
        boolean hasUpperCase = false, hasLowerCase = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpperCase = true;
            else if (Character.isLowerCase(c)) hasLowerCase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return context.getString(R.string.account_password_no_chinese);
            }
        }
        if (!hasUpperCase) return context.getString(R.string.account_password_need_uppercase);
        if (!hasLowerCase) return context.getString(R.string.account_password_need_lowercase);
        if (!hasDigit) return context.getString(R.string.account_password_need_digit);
        return null;
    }

    /**
     * 登录结果回调。{@link AccountManager} 保证在主线程调用，
     * 消费者（ViewModel）可安全更新 LiveData 并触发 UI 观察。
     */
    public interface LoginCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface RegisterCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface PasswordUpdateCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ProfileUpdateCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface AccountDeletionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface LogoutCallback {
        void onSuccess();
        void onError(String message);
    }
}
