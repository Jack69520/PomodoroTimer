package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.AppContainer;
import com.skyinit.pomodorotimer.AppDatabase;
import com.skyinit.pomodorotimer.TestApp;
import com.skyinit.pomodorotimer.data.entity.User;
import com.skyinit.pomodorotimer.util.AppExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = TestApp.class)
public class AccountManagerFlowTest {

    private static final String VALID_PASSWORD = "Abcdef1";
    private static final long CALLBACK_TIMEOUT_MS = 10_000L;

    private Context context;
    private App app;
    private AccountManager accountManager;

    @Before
    public void setUp() throws InterruptedException {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        app = (App) context;
        assertTrue(app.isUserDataInitialized());
        accountManager = AccountManager.getInstance(context);
        assertTrue(accountManager.hasActiveProfile());
        assertTrue(accountManager.isLocalProfile());
    }

    @After
    public void tearDown() {
        // 状态清理由 TestApp.onCreate 统一负责
    }

    @Test
    public void register_upgradeLocalProfile_succeeds() throws InterruptedException {
        String localUserId = accountManager.requireActiveUserId();

        User registered = awaitRegister("Tester", VALID_PASSWORD);
        assertNotNull(registered);
        assertEquals(localUserId, registered.userId);
        assertTrue(accountManager.isRegistered());
        assertEquals(User.ACCOUNT_TYPE_REGISTERED, registered.accountType);
    }

    @Test
    public void login_afterLogout_restoresRegisteredAccount() throws InterruptedException {
        String registeredUserId = awaitRegister("LoginUser", VALID_PASSWORD).userId;

        awaitLogout();
        assertTrue(accountManager.isLocalProfile());
        assertNotEquals(registeredUserId, accountManager.requireActiveUserId());

        User loggedIn = awaitLogin(registeredUserId, VALID_PASSWORD);
        assertEquals(registeredUserId, loggedIn.userId);
        assertTrue(accountManager.isRegistered());
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
        assertTrue(accountManager.isLocalProfile());
    }

    @Test
    public void deleteCurrentAccount_createsFreshLocalProfile() throws InterruptedException {
        awaitRegister("DeleteMe", VALID_PASSWORD);
        String registeredUserId = accountManager.requireActiveUserId();

        awaitDeleteAccount();
        assertTrue(accountManager.isLocalProfile());
        assertNotEquals(registeredUserId, accountManager.requireActiveUserId());

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
    public void register_whenAlreadyRegistered_failsOnMainThread() {
        AtomicReference<String> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            awaitRegister("First", VALID_PASSWORD);
            accountManager.register("Second", VALID_PASSWORD, VALID_PASSWORD,
                    null, null, new AccountManager.RegisterCallback() {
                        @Override
                        public void onSuccess(User user) {
                            fail("Should not register twice");
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e.getMessage());
        }
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
