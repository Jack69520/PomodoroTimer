package com.skyinit.pomodorotimer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import com.skyinit.pomodorotimer.util.AppLog;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.skyinit.pomodorotimer.data.repository.ActiveSessionStore;
import com.skyinit.pomodorotimer.data.repository.PrivacyConsentRepository;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.ui.consent.PrivacyConsentActivity;
import com.skyinit.pomodorotimer.service.AppBlockingService;
import com.skyinit.pomodorotimer.service.TimerService;
import com.skyinit.pomodorotimer.service.TimerServiceLauncher;
import com.skyinit.pomodorotimer.ui.home.CollectionTodoEditActivity;
import com.skyinit.pomodorotimer.ui.home.HomeFragment;
import com.skyinit.pomodorotimer.ui.home.InterruptedSessionDialogHelper;
import com.skyinit.pomodorotimer.ui.home.SimpleTodoEditActivity;
import com.skyinit.pomodorotimer.ui.home.TimerActivity;
import com.skyinit.pomodorotimer.util.ColorContrastUtils;
import com.skyinit.pomodorotimer.util.ExactAlarmPermissionHelper;
import com.skyinit.pomodorotimer.util.FocusBlockNavigation;
import com.skyinit.pomodorotimer.ui.profile.SettingsActivity;
import com.skyinit.pomodorotimer.util.ShortcutActions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.core.content.ContextCompat;

public class MainActivity extends BaseActivity {
    TimerService timerService;
    boolean isBound = false;

    private NavController navController;
    private int selectedNavId = R.id.nav_home;
    private int currentDestinationId = R.id.nav_home;
    private int topLevelBarColor;
    private MaterialToolbar mainToolbar;
    private View mainToolbarContainer;
    private BottomNavigationView bottomNavigationView;
    private ActivityResultLauncher<Intent> taskEditLauncher;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            isBound = true;
            pushServiceToCurrentFragment();
            showStartupDialogsIfNeeded();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            timerService = null;
        }
    };

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void applyThemeStyle() {
        setTheme(R.style.Theme_PomodoroTimer_NoActionBar);
    }

    @Override
    protected void onBarColorApplied(int barColor) {
        topLevelBarColor = barColor;
        if (isTopLevelDestination(currentDestinationId)) {
            applyTopLevelToolbarStyle(barColor);
        }
    }

    private void clearMainToolbarDecoration() {
        if (mainToolbarContainer != null) {
            mainToolbarContainer.setElevation(0f);
            mainToolbarContainer.setTranslationZ(0f);
            mainToolbarContainer.setStateListAnimator(null);
            mainToolbarContainer.setOutlineProvider(null);
        }
        if (mainToolbar != null) {
            mainToolbar.setElevation(0f);
            mainToolbar.setTranslationZ(0f);
            mainToolbar.setStateListAnimator(null);
            mainToolbar.setOutlineProvider(null);
            mainToolbar.setBackground(null);
        }
    }

    private int resolveToolbarBarColor() {
        if (topLevelBarColor != 0) {
            return topLevelBarColor;
        }
        return ContextCompat.getColor(this, R.color.default_theme);
    }

    private void applyTopLevelToolbarStyle(int barColor) {
        clearMainToolbarDecoration();
        if (mainToolbarContainer != null) {
            mainToolbarContainer.setBackgroundColor(barColor);
        }
        if (mainToolbar != null) {
            int titleColor = ColorContrastUtils.getContrastingTextColor(this, barColor);
            mainToolbar.setTitleTextColor(titleColor);
            mainToolbar.setNavigationIconTint(titleColor);
            tintToolbarMenuIcons(titleColor);
        }
    }

    private void tintToolbarMenuIcons(int color) {
        if (mainToolbar == null) {
            return;
        }
        if (mainToolbar.getOverflowIcon() != null) {
            mainToolbar.getOverflowIcon().setTint(color);
        }
        android.graphics.drawable.Drawable addIcon = mainToolbar.getMenu().findItem(R.id.action_add_todo) != null
                ? mainToolbar.getMenu().findItem(R.id.action_add_todo).getIcon()
                : null;
        if (addIcon != null) {
            addIcon.setTint(color);
        }
    }

    private void updateHomeToolbarMenu(int destinationId) {
        if (mainToolbar == null) {
            return;
        }
        mainToolbar.getMenu().clear();
        if (destinationId == R.id.nav_home) {
            mainToolbar.inflateMenu(R.menu.main_home_toolbar_menu);
            int titleColor = ColorContrastUtils.getContrastingTextColor(this, resolveToolbarBarColor());
            tintToolbarMenuIcons(titleColor);
            mainToolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_add_todo) {
                    showAddTodoMenu();
                    return true;
                }
                return false;
            });
        } else {
            mainToolbar.setOnMenuItemClickListener(null);
        }
    }

    private void showAddTodoMenu() {
        PopupMenu popup = new PopupMenu(this, mainToolbar, Gravity.END);
        popup.getMenu().add(0, 1, 0, R.string.add_todo_simple);
        popup.getMenu().add(0, 2, 1, R.string.add_todo_collection);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                taskEditLauncher.launch(SimpleTodoEditActivity.createIntent(this, -1));
                return true;
            }
            if (item.getItemId() == 2) {
                taskEditLauncher.launch(CollectionTodoEditActivity.createIntent(this, -1));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void applySubPageToolbarStyle(int barColor) {
        if (mainToolbarContainer != null) {
            mainToolbarContainer.setBackgroundColor(barColor);
            mainToolbarContainer.setElevation(4f);
        }
        if (mainToolbar != null) {
            mainToolbar.setBackground(null);
            int titleColor = ColorContrastUtils.getContrastingTextColor(this, barColor);
            mainToolbar.setTitleTextColor(titleColor);
            mainToolbar.setNavigationIconTint(titleColor);
        }
    }

    private void setupRootWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (mainToolbarContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainToolbarContainer, (view, windowInsets) -> {
                Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                view.setPadding(0, statusBars.top, 0, 0);
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(mainToolbarContainer);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            final int initialBottomPadding = bottomNav.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, windowInsets) -> {
                Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
                view.setPadding(
                        view.getPaddingLeft(),
                        view.getPaddingTop(),
                        view.getPaddingRight(),
                        initialBottomPadding + navigationBars.bottom
                );
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(bottomNav);
        }
    }

    private int getToolbarTitleRes(int destinationId) {
        if (destinationId == R.id.nav_home) {
            return R.string.nav_title_home;
        }
        if (destinationId == R.id.nav_statistics) {
            return R.string.nav_title_statistics;
        }
        if (destinationId == R.id.nav_calendar) {
            return R.string.nav_title_calendar;
        }
        if (destinationId == R.id.nav_profile) {
            return R.string.nav_title_profile;
        }
        if (destinationId == R.id.nav_session_detail) {
            return R.string.title_session_detail;
        }
        return 0;
    }

    private boolean isTopLevelDestination(int destinationId) {
        return destinationId == R.id.nav_home
                || destinationId == R.id.nav_statistics
                || destinationId == R.id.nav_calendar
                || destinationId == R.id.nav_profile;
    }

    private void updateMainToolbarForDestination(int destinationId) {
        if (mainToolbar == null) {
            return;
        }
        currentDestinationId = destinationId;
        int titleRes = getToolbarTitleRes(destinationId);
        if (titleRes != 0) {
            mainToolbar.setTitle(titleRes);
        }
        if (isTopLevelDestination(destinationId)) {
            mainToolbar.setNavigationIcon(null);
            mainToolbar.setNavigationOnClickListener(null);
            applyTopLevelToolbarStyle(resolveToolbarBarColor());
        } else {
            mainToolbar.setNavigationIcon(R.drawable.ic_back);
            mainToolbar.setNavigationOnClickListener(v -> {
                if (navController != null) {
                    navController.navigateUp();
                }
            });
            applySubPageToolbarStyle(resolveToolbarBarColor());
        }
        if (mainToolbarContainer != null) {
            mainToolbarContainer.setVisibility(View.VISIBLE);
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(isTopLevelDestination(destinationId) ? View.VISIBLE : View.GONE);
        }
        updateHomeToolbarMenu(destinationId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!PrivacyConsentRepository.getInstance(this).hasAccepted()) {
            startActivity(new Intent(this, PrivacyConsentActivity.class));
            finish();
            return;
        }
        App app = (App) getApplication();
        if (!app.isUserDataInitialized()) {
            app.initializeAfterConsent();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainToolbarContainer = findViewById(R.id.main_toolbar_container);
        mainToolbar = findViewById(R.id.main_toolbar);
        taskEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { /* 列表由 Room LiveData 自动刷新 */ });
        clearMainToolbarDecoration();
        setupRootWindowInsets();

        if (savedInstanceState != null) {
            selectedNavId = savedInstanceState.getInt("SELECTED_NAV", R.id.nav_home);
        }

        NavHostFragment navHostFragment = getNavHostFragment();
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNavigationView = bottomNav;
        if (navController != null) {
            NavigationUI.setupWithNavController(bottomNav, navController);
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                selectedNavId = destination.getId();
                if (isTopLevelDestination(destination.getId())) {
                    selectedNavId = destination.getId();
                }
                updateMainToolbarForDestination(destination.getId());
                pushServiceToCurrentFragment();
            });
        }
        bottomNav.setSelectedItemId(selectedNavId);
        if (navController != null && navController.getCurrentDestination() != null) {
            updateMainToolbarForDestination(navController.getCurrentDestination().getId());
        } else {
            updateMainToolbarForDestination(selectedNavId);
        }
        applyBottomNavRipple(bottomNav);
        applyTheme();
        handleNavigationIntent(getIntent());
        handleShortcutIntent(getIntent());

        checkAndRequestPermissions();

        TimerServiceLauncher.ensureRunning(this);
        bindService(new Intent(this, TimerService.class), connection, Context.BIND_AUTO_CREATE);
    }

    /** 启动后弹窗：优先处理中断番茄钟，否则在应用内说明精确闹钟（不自动跳转设置）。 */
    private void showStartupDialogsIfNeeded() {
        if (ActiveSessionStore.hasActiveSession(this)) {
            InterruptedSessionDialogHelper.showIfNeeded(this);
        } else {
            ExactAlarmPermissionHelper.maybeShowInAppDialog(this);
        }
    }

    private NavHostFragment getNavHostFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        return fragment instanceof NavHostFragment ? (NavHostFragment) fragment : null;
    }

    private void pushServiceToCurrentFragment() {
        if (!isBound || timerService == null) return;
        NavHostFragment navHostFragment = getNavHostFragment();
        if (navHostFragment == null) return;
        Fragment current = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
        if (current == null && !navHostFragment.getChildFragmentManager().getFragments().isEmpty()) {
            current = navHostFragment.getChildFragmentManager().getFragments().get(0);
        }
        if (current instanceof HomeFragment) {
            ((HomeFragment) current).setTimerService(timerService);
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        boolean needRequest = false;
        String[] permissionsToRequest = new String[]{};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needRequest = true;
            permissionsToRequest = new String[]{Manifest.permission.POST_NOTIFICATIONS};
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (needRequest) {
                String[] newPermissions = new String[permissionsToRequest.length + 1];
                System.arraycopy(permissionsToRequest, 0, newPermissions, 0, permissionsToRequest.length);
                newPermissions[permissionsToRequest.length] = Manifest.permission.READ_MEDIA_AUDIO;
                permissionsToRequest = newPermissions;
            } else {
                needRequest = true;
                permissionsToRequest = new String[]{Manifest.permission.READ_MEDIA_AUDIO};
            }
        }
        if (needRequest && permissionsToRequest.length > 0) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (Manifest.permission.POST_NOTIFICATIONS.equals(permissions[i])) {
                        Toast.makeText(this, R.string.common_toast_notification_permission_denied, Toast.LENGTH_LONG).show();
                    } else if (Manifest.permission.READ_MEDIA_AUDIO.equals(permissions[i])) {
                        Toast.makeText(this, R.string.common_toast_audio_permission_denied, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isBound) {
            TimerServiceLauncher.ensureRunning(this);
            bindService(new Intent(this, TimerService.class), connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pushServiceToCurrentFragment();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
        handleShortcutIntent(intent);
    }

    private void handleShortcutIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String shortcutAction = intent.getStringExtra(ShortcutActions.EXTRA_SHORTCUT_ACTION);
        if (shortcutAction == null) {
            return;
        }
        intent.removeExtra(ShortcutActions.EXTRA_SHORTCUT_ACTION);

        switch (shortcutAction) {
            case ShortcutActions.ACTION_START_FOCUS_25:
                startFocus25FromShortcut();
                break;
            case ShortcutActions.ACTION_VIEW_STATISTICS:
                navigateToDestination(R.id.nav_statistics);
                break;
            case ShortcutActions.ACTION_ENABLE_BLOCKING:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra(SettingsActivity.EXTRA_ENABLE_APP_BLOCKING, true);
                startActivity(settingsIntent);
                break;
            default:
                break;
        }
    }

    private void startFocus25FromShortcut() {
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.setAction(TimerService.ACTION_START);
        serviceIntent.putExtra(TimerService.EXTRA_STUDY_DURATION_MS, 25L * 60L * 1000L);
        TimerServiceLauncher.deliverAction(this, serviceIntent);
        startActivity(new Intent(this, TimerActivity.class));
    }

    private void navigateToDestination(int destinationId) {
        if (navController == null) {
            return;
        }
        navController.navigate(destinationId);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(destinationId);
        }
        selectedNavId = destinationId;
        updateMainToolbarForDestination(destinationId);
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null || navController == null) {
            return;
        }
        int destinationId = intent.getIntExtra(FocusBlockNavigation.EXTRA_NAV_DESTINATION, 0);
        if (destinationId == 0) {
            return;
        }
        navController.navigate(destinationId);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(destinationId);
        }
        selectedNavId = destinationId;
        updateMainToolbarForDestination(destinationId);
        intent.removeExtra(FocusBlockNavigation.EXTRA_NAV_DESTINATION);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ensureAppBlockingServiceTimeout();
    }

    @Override
    protected void onDestroy() {
        if (isBound) {
            unbindService(connection);
            isBound = false;
            timerService = null;
        }
        super.onDestroy();
    }

    public int getSelectedNavigationId() {
        return selectedNavId;
    }

    public TimerService getTimerService() {
        return timerService;
    }

    private void ensureAppBlockingServiceTimeout() {
        try {
            Intent intent = new Intent(this, AppBlockingService.class);
            intent.putExtra("action", "check_timeout");
            startService(intent);
        } catch (Exception e) {
            AppLog.e("MainActivity", "Error ensuring service timeout", e);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("SELECTED_NAV", selectedNavId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedNavId = savedInstanceState.getInt("SELECTED_NAV", R.id.nav_home);
    }

    private void applyBottomNavRipple(BottomNavigationView bottomNav) {
        if (bottomNav == null) {
            return;
        }
        bottomNav.post(() -> {
            if (bottomNav.getChildCount() == 0) {
                return;
            }
            View menuView = bottomNav.getChildAt(0);
            if (!(menuView instanceof ViewGroup)) {
                return;
            }

            int rippleColor = ContextCompat.getColor(this, R.color.bottom_nav_ripple);
            int radiusPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    18f,
                    getResources().getDisplayMetrics()
            );

            ViewGroup menuGroup = (ViewGroup) menuView;
            for (int i = 0; i < menuGroup.getChildCount(); i++) {
                Drawable background = menuGroup.getChildAt(i).getBackground();
                if (background instanceof RippleDrawable) {
                    RippleDrawable ripple = (RippleDrawable) background;
                    ripple.setColor(ColorStateList.valueOf(rippleColor));
                    ripple.setRadius(radiusPx);
                }
            }
        });
    }

    public void refreshBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            applyBottomNavRipple(bottomNav);
            bottomNav.invalidate();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SettingsManager settings = new SettingsManager(this);
                int themeColor = settings.getThemeColor();
                try {
                    String resourceType = getResources().getResourceTypeName(themeColor);
                    if ("color".equals(resourceType)) {
                        getWindow().setNavigationBarColor(ContextCompat.getColor(this, themeColor));
                    }
                } catch (Exception e) {
                    getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.default_theme));
                }
            }
        }
    }

    public void switchToHome() {
        if (navController != null) {
            navController.navigate(R.id.nav_home);
        }
    }
}
