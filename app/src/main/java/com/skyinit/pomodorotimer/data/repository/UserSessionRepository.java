package com.skyinit.pomodorotimer.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.skyinit.pomodorotimer.data.entity.User;

/**
 * 可观察的用户会话层：对外暴露活跃档案 LiveData，UI 与 ViewModel 应通过本类访问账户状态。
 */
public final class UserSessionRepository {

    private final AccountManager accountManager;
    private final UserPomodoroSettingsRepository pomodoroSettingsRepository;
    private final MutableLiveData<User> activeUser = new MutableLiveData<>();
    private final MutableLiveData<Integer> sessionVersion = new MutableLiveData<>(0);

    public UserSessionRepository(AccountManager accountManager,
                                 UserPomodoroSettingsRepository pomodoroSettingsRepository) {
        this.accountManager = accountManager;
        this.pomodoroSettingsRepository = pomodoroSettingsRepository;
        accountManager.setActiveUserListener((user, sessionSwitched) -> {
            activeUser.postValue(user);
            if (sessionSwitched) {
                pomodoroSettingsRepository.invalidateCache();
                pomodoroSettingsRepository.warmCache();
                Integer current = sessionVersion.getValue();
                sessionVersion.postValue(current == null ? 1 : current + 1);
            }
        });
    }

    /** 冷启动在 {@link AccountManager#ensureDefaultProfile()} 之后调用，同步初始活跃档案。 */
    public void syncFromAccountManager() {
        User user = accountManager.getCurrentUser();
        if (user != null) {
            activeUser.postValue(user);
        }
    }

    public LiveData<User> getActiveUser() {
        return activeUser;
    }

    /**
     * 活跃档案切换时递增（登出、登录、注销等）；资料就地更新不递增。
     */
    public LiveData<Integer> getSessionVersion() {
        return sessionVersion;
    }

    @NonNull
    public String requireActiveUserId() {
        return accountManager.requireActiveUserId();
    }

    public User getCurrentUser() {
        return accountManager.getCurrentUser();
    }

    public boolean hasActiveProfile() {
        return accountManager.hasActiveProfile();
    }

    public boolean isLocalProfile() {
        return accountManager.isLocalProfile();
    }

    public boolean isRegistered() {
        return accountManager.isRegistered();
    }

    public boolean isForcePasswordReset() {
        return accountManager.isForcePasswordReset();
    }

    public void clearForcePasswordReset() {
        accountManager.clearForcePasswordReset();
    }

    public void login(String userId, String password, AccountManager.LoginCallback callback) {
        accountManager.login(userId, password, callback);
    }

    public void recoverLogin(String userId, String nickname, AccountManager.LoginCallback callback) {
        accountManager.recoverLogin(userId, nickname, callback);
    }

    public void register(String nickname, String password, String confirmPassword,
                         String avatarPath, String signature, AccountManager.RegisterCallback callback) {
        accountManager.register(nickname, password, confirmPassword, avatarPath, signature, callback);
    }

    public void logout(AccountManager.LogoutCallback callback) {
        accountManager.logout(callback);
    }

    public void deleteCurrentAccount(AccountManager.AccountDeletionCallback callback) {
        accountManager.deleteCurrentAccount(callback);
    }

    public void updateProfile(String nickname, String avatarPath, String signature,
                              AccountManager.ProfileUpdateCallback callback) {
        accountManager.updateProfile(nickname, avatarPath, signature, callback);
    }

    public void updatePassword(String oldPassword, String newPassword, String confirmPassword,
                               AccountManager.PasswordUpdateCallback callback) {
        accountManager.updatePassword(oldPassword, newPassword, confirmPassword, callback);
    }
}
