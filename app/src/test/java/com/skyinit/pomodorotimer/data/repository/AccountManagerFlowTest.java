package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.data.AvatarStorage;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.security.PasswordHasher;
import com.skyinit.pomodorotimer.util.AppExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Guest / Registered 账户流回归：冷启动为 Guest，注册登录后可恢复，登出/删除回到 Guest。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class AccountManagerFlowTest {

    private static final String VALID_PASSWORD = "Abcdef1";
    private static final long CALLBACK_TIMEOUT_MS = 10_000L;

    private Context context;
    private AccountManager accountManager;

    @Before
    public void setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        App app = (App) context;
        assertTrue(app.isUserDataInitialized());
        accountManager = AccountManager.getInstance(context);
        assertFalse("Cold start should be Guest", accountManager.hasActiveSession());
        assertFalse(accountManager.isLoggedIn());
        assertNull(accountManager.getCurrentUserId());
    }

    @After
    public void tearDown() {
        // 状态清理由 TestApp.onCreate 统一负责
    }

    @Test
    public void register_fromGuest_createsLoggedInAccount() throws InterruptedException {
        User registered = awaitRegister("Tester", VALID_PASSWORD);
        assertNotNull(registered);
        assertNotNull(registered.userId);
        assertFalse(registered.userId.isEmpty());
        assertTrue(accountManager.isLoggedIn());
        assertTrue(accountManager.hasActiveSession());
        assertTrue(accountManager.isRegistered());
    }

    @Test
    public void login_afterLogout_restoresRegisteredAccount() throws InterruptedException {
        String registeredUserId = awaitRegister("LoginUser", VALID_PASSWORD).userId;

        awaitLogout();
        assertFalse(accountManager.hasActiveSession());
        assertNull(accountManager.getCurrentUserId());

        User loggedIn = awaitLogin(registeredUserId, VALID_PASSWORD);
        assertTrue(registeredUserId.equals(loggedIn.userId));
        assertTrue(accountManager.isLoggedIn());
    }

    @Test
    public void login_withWrongPassword_fails() throws InterruptedException {
        String registeredUserId = awaitRegister("WrongPass", VALID_PASSWORD).userId;
        awaitLogout();

        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        accountManager.login(registeredUserId, "WrongPw1", new AccountManager.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                fail("Expected login failure");
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                error.set(message);
                latch.countDown();
            }
        });
        awaitCallback(latch);
        assertNotNull(error.get());
        assertFalse(error.get().isEmpty());
        assertFalse(accountManager.hasActiveSession());
    }

    @Test
    public void deleteCurrentAccount_returnsToGuest() throws InterruptedException {
        awaitRegister("DeleteMe", VALID_PASSWORD);
        String registeredUserId = accountManager.requireActiveUserId();

        awaitDeleteAccount();
        assertFalse(accountManager.hasActiveSession());
        assertNull(accountManager.getCurrentUserId());

        AtomicReference<User> deletedCheck = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        AppExecutors.getInstance().diskIo(() -> {
            deletedCheck.set(AppDatabase.getDatabase(context).userDao().getUserById(registeredUserId));
            latch.countDown();
        });
        awaitCallback(latch);
        assertNull(deletedCheck.get());
    }

    @Test
    public void deleteCurrentAccount_removesAvatarFile_otherUserUnaffected()
            throws InterruptedException {
        User userA = awaitRegister("AvatarA", VALID_PASSWORD);
        String userIdA = userA.userId;
        Bitmap bitmapA = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        bitmapA.eraseColor(Color.RED);
        String pathA = AvatarStorage.getInstance().saveJpeg(context, userIdA, bitmapA);
        bitmapA.recycle();
        assertNotNull(pathA);
        assertTrue(new File(pathA).exists());

        awaitLogout();
        User userB = awaitRegister("AvatarB", VALID_PASSWORD);
        String userIdB = userB.userId;
        Bitmap bitmapB = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        bitmapB.eraseColor(Color.BLUE);
        String pathB = AvatarStorage.getInstance().saveJpeg(context, userIdB, bitmapB);
        bitmapB.recycle();
        assertNotNull(pathB);
        assertTrue(new File(pathB).exists());

        awaitLogout();
        awaitLogin(userIdA, VALID_PASSWORD);
        awaitDeleteAccount();

        assertFalse(AvatarStorage.getInstance().getAvatarFile(context, userIdA).exists());
        assertTrue(AvatarStorage.getInstance().getAvatarFile(context, userIdB).exists());
    }

    @Test
    public void updatePassword_persistsToDbBeforeSuccess_oldPasswordCannotLogin()
            throws InterruptedException {
        String userId = awaitRegister("ChangePw", VALID_PASSWORD).userId;
        String newPassword = "Newpass1";

        awaitUpdatePassword(VALID_PASSWORD, newPassword);
        assertFalse(accountManager.isForcePasswordReset());

        AtomicReference<User> persisted = new AtomicReference<>();
        CountDownLatch readLatch = new CountDownLatch(1);
        AppExecutors.getInstance().diskIo(() -> {
            persisted.set(AppDatabase.getDatabase(context).userDao().getUserById(userId));
            readLatch.countDown();
        });
        awaitCallback(readLatch);

        User dbUser = persisted.get();
        assertNotNull(dbUser);
        assertTrue(PasswordHasher.verifyPassword(newPassword, dbUser.password));
        assertFalse(PasswordHasher.verifyPassword(VALID_PASSWORD, dbUser.password));

        awaitLogout();
        awaitLogin(userId, newPassword);

        awaitLogout();
        AtomicReference<String> oldLoginError = new AtomicReference<>();
        CountDownLatch loginOldLatch = new CountDownLatch(1);
        accountManager.login(userId, VALID_PASSWORD, new AccountManager.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                fail("Old password must not login after change");
                loginOldLatch.countDown();
            }

            @Override
            public void onError(String message) {
                oldLoginError.set(message);
                loginOldLatch.countDown();
            }
        });
        awaitCallback(loginOldLatch);
        assertNotNull(oldLoginError.get());
    }

    @Test
    public void register_whenAlreadyLoggedIn_fails() throws InterruptedException {
        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        awaitRegister("First", VALID_PASSWORD);
        accountManager.register("Second", VALID_PASSWORD, VALID_PASSWORD,
                null, null, new AccountManager.RegisterCallback() {
                    @Override
                    public void onSuccess(User user) {
                        fail("Should not register while logged in");
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                        latch.countDown();
                    }
                });
        awaitCallback(latch);
        assertNotNull(error.get());
    }

    private User awaitRegister(String nickname, String password) throws InterruptedException {
        AtomicReference<User> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        accountManager.register(nickname, password, password, null, null,
                new AccountManager.RegisterCallback() {
                    @Override
                    public void onSuccess(User user) {
                        result.set(user);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                        latch.countDown();
                    }
                });
        awaitCallback(latch);
        if (error.get() != null) {
            fail("Register failed: " + error.get());
        }
        return result.get();
    }

    private User awaitLogin(String userId, String password) throws InterruptedException {
        AtomicReference<User> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        accountManager.login(userId, password, new AccountManager.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                result.set(user);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                error.set(message);
                latch.countDown();
            }
        });
        awaitCallback(latch);
        if (error.get() != null) {
            fail("Login failed: " + error.get());
        }
        return result.get();
    }

    private void awaitUpdatePassword(String oldPassword, String newPassword)
            throws InterruptedException {
        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        accountManager.updatePassword(oldPassword, newPassword, newPassword,
                new AccountManager.PasswordUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                        latch.countDown();
                    }
                });
        awaitCallback(latch);
        if (error.get() != null) {
            fail("Update password failed: " + error.get());
        }
    }

    private void awaitLogout() throws InterruptedException {
        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        accountManager.logout(new AccountManager.LogoutCallback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                error.set(message);
                latch.countDown();
            }
        });
        awaitCallback(latch);
        if (error.get() != null) {
            fail("Logout failed: " + error.get());
        }
    }

    private void awaitDeleteAccount() throws InterruptedException {
        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        accountManager.deleteCurrentAccount(new AccountManager.AccountDeletionCallback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                error.set(message);
                latch.countDown();
            }
        });
        awaitCallback(latch);
        if (error.get() != null) {
            fail("Delete account failed: " + error.get());
        }
    }

    private void awaitCallback(CountDownLatch latch) throws InterruptedException {
        long deadlineMs = System.currentTimeMillis() + CALLBACK_TIMEOUT_MS;
        while (latch.getCount() > 0 && System.currentTimeMillis() < deadlineMs) {
            ShadowLooper.idleMainLooper();
            Thread.sleep(20L);
        }
        if (latch.getCount() > 0) {
            fail("Callback timed out");
        }
        ShadowLooper.idleMainLooper();
    }
}
