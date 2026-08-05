package com.skyinit.pomodorotimer.ui.profile;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.repository.BlockedAppRepository;
import com.skyinit.pomodorotimer.domain.blocking.BlockingRole;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用屏蔽管理页 ViewModel：筛选、扫描与开关写入均在此编排；UI 仅观察状态与一次性事件。
 */
public class AppBlockingViewModel extends ViewModel {

    private static final long SEARCH_DEBOUNCE_MS = 280L;

    public static final class UiState {
        public final List<BlockedApp> filteredApps;
        public final int totalCount;
        public final int blockedCount;
        public final int whitelistCount;
        public final int visibleCount;
        public final boolean scanning;
        public final boolean showWhitelistOnly;
        public final boolean hasAnyApps;

        public UiState(List<BlockedApp> filteredApps,
                       int totalCount,
                       int blockedCount,
                       int whitelistCount,
                       int visibleCount,
                       boolean scanning,
                       boolean showWhitelistOnly,
                       boolean hasAnyApps) {
            this.filteredApps = filteredApps;
            this.totalCount = totalCount;
            this.blockedCount = blockedCount;
            this.whitelistCount = whitelistCount;
            this.visibleCount = visibleCount;
            this.scanning = scanning;
            this.showWhitelistOnly = showWhitelistOnly;
            this.hasAnyApps = hasAnyApps;
        }
    }

    /** 一次性 UI 事件，消费后应 clear。 */
    public static final class UiEvent {
        public final String code;
        public final boolean isError;
        @Nullable public final String appName;
        public final int newCount;
        public final int updatedCount;

        public UiEvent(String code, boolean isError) {
            this(code, isError, null, 0, 0);
        }

        public UiEvent(String code, boolean isError, @Nullable String appName) {
            this(code, isError, appName, 0, 0);
        }

        public UiEvent(String code, boolean isError, @Nullable String appName,
                       int newCount, int updatedCount) {
            this.code = code;
            this.isError = isError;
            this.appName = appName;
            this.newCount = newCount;
            this.updatedCount = updatedCount;
        }
    }

    private final BlockedAppRepository repository;
    private final String userId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger toggleGeneration = new AtomicInteger();

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> categoryFilter = new MutableLiveData<>(AppCategory.FILTER_ALL);
    private final MutableLiveData<Boolean> showWhitelistOnly = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> scanning = new MutableLiveData<>(false);
    private final MutableLiveData<UiEvent> uiEvent = new MutableLiveData<>();
    private final MediatorLiveData<UiState> uiState = new MediatorLiveData<>();

    private List<BlockedApp> allApps = new ArrayList<>();
    private Runnable pendingSearchPublish;
    private String pendingSearchText = "";

    public AppBlockingViewModel(BlockedAppRepository repository, String userId) {
        this.repository = repository;
        this.userId = userId;

        LiveData<List<BlockedApp>> source = repository.observeApps(userId);
        uiState.addSource(source, apps -> {
            allApps = apps != null ? new ArrayList<>(apps) : new ArrayList<>();
            publishUiState();
        });
        uiState.addSource(searchQuery, q -> publishUiState());
        uiState.addSource(categoryFilter, c -> publishUiState());
        uiState.addSource(showWhitelistOnly, w -> publishUiState());
        uiState.addSource(scanning, s -> publishUiState());
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<UiEvent> getUiEvent() {
        return uiEvent;
    }

    public void setSearchQueryDebounced(String query) {
        pendingSearchText = query != null ? query.trim().toLowerCase() : "";
        if (pendingSearchPublish != null) {
            mainHandler.removeCallbacks(pendingSearchPublish);
        }
        pendingSearchPublish = () -> searchQuery.setValue(pendingSearchText);
        mainHandler.postDelayed(pendingSearchPublish, SEARCH_DEBOUNCE_MS);
    }

    public void setSearchQueryImmediate(String query) {
        if (pendingSearchPublish != null) {
            mainHandler.removeCallbacks(pendingSearchPublish);
            pendingSearchPublish = null;
        }
        searchQuery.setValue(query != null ? query.trim().toLowerCase() : "");
    }

    public void setCategoryFilter(String category) {
        categoryFilter.setValue(category);
    }

    public void setShowWhitelistOnly(boolean showWhitelist) {
        showWhitelistOnly.setValue(showWhitelist);
    }

    public void checkAutoScan() {
        AppExecutors.getInstance().diskIo(() -> {
            int count = repository.getAppCount(userId);
            if (count == 0) {
                scanInstalledApps();
            } else {
                postEvent(new UiEvent("SCAN_HINT", false));
            }
        });
    }

    public void scanInstalledApps() {
        if (Boolean.TRUE.equals(scanning.getValue())) {
            postEvent(new UiEvent("SCAN_BUSY", false));
            return;
        }
        scanning.postValue(true);
        AppExecutors.getInstance().diskIo(() -> {
            try {
                BlockedAppRepository.ScanResult result = repository.scanAndSync(userId);
                if (result.busy) {
                    postEvent(new UiEvent("SCAN_BUSY", false));
                } else if (result.newCount == 0
                        && result.updatedCategoryCount == 0
                        && result.removedCount == 0) {
                    postEvent(new UiEvent("SCAN_NO_CHANGE", false));
                } else {
                    postEvent(new UiEvent(
                            "SCAN_COMPLETE",
                            false,
                            null,
                            result.newCount,
                            result.updatedCategoryCount));
                }
            } catch (Exception e) {
                postEvent(new UiEvent("SCAN_FAILED", true));
            } finally {
                scanning.postValue(false);
            }
        });
    }

    /**
     * 单一开关：isBlocked=true 表示专注时屏蔽；false 表示放行（白名单）。
     */
    public void updateBlockingStatus(BlockedApp app, boolean isBlocked) {
        if (app == null || app.packageName == null) {
            return;
        }
        if (isCritical(app)) {
            postEvent(new UiEvent("CRITICAL_LOCKED", true));
            return;
        }
        final String packageName = app.packageName;
        final String appName = app.appName != null ? app.appName : packageName;
        final boolean enabled = isBlocked;
        final boolean whitelisted = !isBlocked;
        final int gen = toggleGeneration.incrementAndGet();

        AppExecutors.getInstance().diskIo(() -> {
            if (gen != toggleGeneration.get()) {
                // 被更新的连点覆盖时仍执行最新写入；旧世代不发成功 toast
            }
            boolean ok = repository.updateBlockingFlags(userId, packageName, enabled, whitelisted);
            if (!ok) {
                postEvent(new UiEvent("CRITICAL_LOCKED", true));
            } else if (gen == toggleGeneration.get()) {
                postEvent(new UiEvent(isBlocked ? "TOGGLE_BLOCKED" : "TOGGLE_ALLOWED", false, appName));
            }
        });
    }

    public void clearEvent() {
        uiEvent.setValue(null);
    }

    private boolean isCritical(BlockedApp app) {
        return BlockingRole.CRITICAL == BlockingRole.fromStorage(app.blockingRole)
                || repository.getPolicyEngine().isCritical(app.packageName);
    }

    private void postEvent(UiEvent event) {
        uiEvent.postValue(event);
    }

    private void publishUiState() {
        String query = searchQuery.getValue() != null ? searchQuery.getValue() : "";
        String category = categoryFilter.getValue() != null
                ? categoryFilter.getValue() : AppCategory.FILTER_ALL;
        boolean whitelistOnly = Boolean.TRUE.equals(showWhitelistOnly.getValue());
        boolean isScanning = Boolean.TRUE.equals(scanning.getValue());

        List<BlockedApp> filtered = new ArrayList<>();
        int blockedCount = 0;
        int whitelistCount = 0;

        for (BlockedApp app : allApps) {
            if (app.isEnabled) {
                blockedCount++;
            }
            if (app.isWhitelisted) {
                whitelistCount++;
            }

            boolean matchesSearch = query.isEmpty()
                    || (app.appName != null && app.appName.toLowerCase().contains(query))
                    || (app.packageName != null && app.packageName.toLowerCase().contains(query));
            boolean matchesCategory = AppCategory.FILTER_ALL.equals(category)
                    || category.equals(app.category);
            boolean matchesWhitelist = !whitelistOnly || app.isWhitelisted;

            if (matchesSearch && matchesCategory && matchesWhitelist) {
                filtered.add(app);
            }
        }

        uiState.setValue(new UiState(
                Collections.unmodifiableList(filtered),
                allApps.size(),
                blockedCount,
                whitelistCount,
                filtered.size(),
                isScanning,
                whitelistOnly,
                !allApps.isEmpty()
        ));
    }

    @Override
    protected void onCleared() {
        if (pendingSearchPublish != null) {
            mainHandler.removeCallbacks(pendingSearchPublish);
        }
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
