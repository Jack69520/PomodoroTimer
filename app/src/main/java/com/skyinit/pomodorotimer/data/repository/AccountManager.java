package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.PasswordRepository;
import com.skyinit.pomodorotimer.data.dao.UserDao;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.domain.account.UserIdGenerator;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.AppLog;

import java.util.function.Consumer;

/**
 * 注册账户会话管理：Guest（无活跃用户）或已登录注册用户；永不自动创建本地档案。
 */
public class AccountManager {
    private static final String PREFS_NAME = "account_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_FORCE_PASSWORD_RESET = "force_password_reset";

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
        void onActiveUserChanged(@Nullable User user, boolean sessionSwitched);
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

    public void setTimerStateRepository(TimerStateRepository timerStateRepository) {
        timerStateGuard.setTimerStateRepository(timerStateRepository);
    }

    /**
     * 冷启动：若 prefs 有合法已注册用户则恢复，否则保持 Guest。禁止创建本地档案。
     */
    public void restoreSessionOnDisk() {
        if (currentUser != null) {
            return;
        }
        String userId = prefs.getString(KEY_CURRENT_USER_ID, null);
        if (userId == null || userId.isEmpty()) {
            clearSessionOnDisk(false);
            return;
        }
        User user = userDao.getUserById(userId);
        if (user == null) {
            clearSessionOnDisk(false);
            return;
        }
        activateUserOnDisk(user, false);
    }

    public void restoreSessionAsync(@NonNull Runnable onSuccess, @NonNull Consumer<Throwable> onError) {
        appExecutors.diskIo(() -> {
            try {
                restoreSessionOnDisk();
                mainHandler.post(onSuccess);
            } catch (Throwable t) {
                AppLog.e("AccountManager", "restoreSessionAsync failed", t);
                mainHandler.post(() -> onError.accept(t));
            }
        }, t -> mainHandler.post(() -> onError.accept(t)));
    }

    @Nullable
    public String getCurrentUserId() {
        return currentUser != null ? currentUser.userId : null;
    }

    @Nullable
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean hasActiveSession() {
        return currentUser != null;
    }

    /** @deprecated 使用 {@link #hasActiveSession()} */
    @Deprecated
    public boolean hasActiveProfile() {
        return hasActiveSession();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** @deprecated 使用 {@link #isLoggedIn()} */
    @Deprecated
    public boolean isRegistered() {
        return isLoggedIn();
    }

    @NonNull
    public String requireActiveUserId() {
        User user = currentUser;
        if (user == null) {
            throw new IllegalStateException("No active registered session");
        }
        return user.userId;
    }

    @NonNull
    public User requireCurrentUser() {
        User user = currentUser;
        if (user == null) {
            throw new IllegalStateException("No active registered session");
        }
        return user;
    }

    public void login(String userId, String password, LoginCallback callback) {
        appExecutors.diskIo(() -> {
            try {
                if (hasActiveTimerSession()) {
                    dispatchLoginError(callback, getActiveTimerSessionMessage());
                    return;
                }
                String normalizedId = userId != null ? userId.trim() : "";
                if (!UserIdGenerator.isValidFormat(normalizedId)) {
                    dispatchLoginError(callback,
                            context.getString(R.string.account_error_login_invalid));
                    return;
                }
                User user = userDao.getUserById(normalizedId);
                if (user == null || !passwordRepository.verifyPassword(user.password, password)) {
                    dispatchLoginError(callback,
                            context.getString(R.string.account_error_login_invalid));
                    return;
                }
                userDao.updateLastLoginTime(normalizedId, System.currentTimeMillis());
                User loggedInUser = userDao.getUserById(normalizedId);
                activateUserOnDisk(loggedInUser, true);
                dispatchLoginSuccess(callback, loggedInUser);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Login error", e);
                dispatchLoginError(callback,
                        context.getString(R.string.account_error_login_failed, e.getMessage()));
            }
        });
    }

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
                String normalizedId = userId != null ? userId.trim() : "";
                if (!UserIdGenerator.isValidFormat(normalizedId)) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_recovery_mismatch)));
                    return;
                }
                User user = userDao.getUserByIdAndNickname(normalizedId, nickname);
                if (user == null) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_recovery_mismatch)));
                    return;
                }
                prefs.edit().putBoolean(KEY_FORCE_PASSWORD_RESET, true).commit();
                userDao.updateLastLoginTime(normalizedId, System.currentTimeMillis());
                User loggedInUser = userDao.getUserById(normalizedId);
                activateUserOnDisk(loggedInUser, true);
                mainHandler.post(() -> callback.onSuccess(loggedInUser));
            } catch (Exception e) {
                AppLog.e("AccountManager", "Recover login error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_recovery_failed, e.getMessage())));
            }
        });
    }

    public boolean isForcePasswordReset() {
        return prefs.getBoolean(KEY_FORCE_PASSWORD_RESET, false);
    }

    public void clearForcePasswordReset() {
        // commit：与 recoverLogin 设 flag 对称，避免改密成功后进程被杀仍残留强制改密态
        prefs.edit().putBoolean(KEY_FORCE_PASSWORD_RESET, false).commit();
    }

    /**
     * 仅 Guest 可注册：创建新 12 位 ID 用户并激活。已登录必须先登出。
     */
    public void register(String nickname, String password, String confirmPassword,
                         String avatarPath, String signature, RegisterCallback callback) {
        appExecutors.diskIo(() -> {
            try {
                if (currentUser != null) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_logout_before_register)));
                    return;
                }
                if (hasActiveTimerSession()) {
                    mainHandler.post(() -> callback.onError(getActiveTimerSessionMessage()));
                    return;
                }
                if (nickname == null || nickname.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_nickname_required)));
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
                String userId = UserIdGenerator.generateUnique(
                        id -> userDao.checkUserIdExists(id) > 0);
                User user = new User();
                user.userId = userId;
                user.nickname = nickname.trim();
                user.avatarPath = avatarPath;
                user.signature = signature != null ? signature : "";
                user.createdAt = System.currentTimeMillis();
                user.lastLoginAt = System.currentTimeMillis();
                passwordRepository.hashAndStorePassword(context, user, password);
                userDao.insert(user);
                database.userAppBlockingDao().upsert(
                        new com.skyinit.pomodorotimer.data.entity.UserAppBlocking(userId, false));
                activateUserOnDisk(userDao.getUserById(userId), true);
                mainHandler.post(() -> callback.onSuccess(currentUser));
            } catch (Exception e) {
                AppLog.e("AccountManager", "Register error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_register_failed, e.getMessage())));
            }
        });
    }

    public void logout(LogoutCallback callback) {
        appExecutors.diskIo(() -> {
            try {
                if (currentUser == null) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_not_logged_in)));
                    return;
                }
                if (hasActiveTimerSession()) {
                    mainHandler.post(() -> callback.onError(getActiveTimerSessionMessage()));
                    return;
                }
                clearSessionOnDisk(true);
                clearForcePasswordReset();
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Logout error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_logout_failed, e.getMessage())));
            }
        });
    }

    public void deleteCurrentAccount(AccountDeletionCallback callback) {
        if (currentUser == null) {
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
                    database.blockedAppDao().deleteByUserId(userId);
                    database.userAppBlockingDao().deleteByUserId(userId);
                    userDao.deleteUserById(userId);
                });

                if (userDao.getUserById(userId) != null) {
                    mainHandler.post(() -> callback.onError(
                            context.getString(R.string.account_error_delete_failed_retry)));
                    return;
                }

                passwordRepository.clearStoredSalt(context, userId);
                clearSessionOnDisk(true);
                clearForcePasswordReset();
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
        if (currentUser == null) {
            callback.onError(context.getString(R.string.account_error_login_required));
            return;
        }
        appExecutors.diskIo(() -> {
            try {
                boolean forceReset = isForcePasswordReset();
                if (!forceReset && !passwordRepository.verifyPassword(
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
                // 同 diskIo 任务内同步落库；成功语义 = Room 已更新，再清强制改密并 ACK
                passwordRepository.updatePasswordOnDisk(context, currentUser, newPassword);
                clearForcePasswordReset();
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                AppLog.e("AccountManager", "Update password error", e);
                mainHandler.post(() -> callback.onError(
                        context.getString(R.string.account_error_password_update_failed, e.getMessage())));
            }
        });
    }

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

    private boolean hasActiveTimerSession() {
        return timerStateGuard.hasActiveTimerState();
    }

    private String getActiveTimerSessionMessage() {
        return timerStateGuard.getBlockedMessage();
    }

    private void activateUserOnDisk(@NonNull User user, boolean sessionSwitched) {
        currentUser = user;
        prefs.edit()
                .putString(KEY_CURRENT_USER_ID, user.userId)
                .commit();
        notifyActiveUserChanged(sessionSwitched);
    }

    private void clearSessionOnDisk(boolean sessionSwitched) {
        currentUser = null;
        prefs.edit().remove(KEY_CURRENT_USER_ID).commit();
        notifyActiveUserChanged(sessionSwitched);
    }

    private void notifyActiveUserChanged(boolean sessionSwitched) {
        if (activeUserListener == null) {
            return;
        }
        User user = currentUser;
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
