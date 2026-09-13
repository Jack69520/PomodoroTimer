package com.skyinit.pomodorotimer.ui.profile;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
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
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用屏蔽管理页 ViewModel：筛选、扫描与开关写入均在此编排；UI 仅观察状态与一次性事件。
 */
public class AppBlockingViewModel extends ViewModel {

    private static final long SEARCH_DEBOUNCE_MS = 280L;

    public static final class UiState {
        public final List<BlockedApp> filteredApps;
        /** 当前搜索+分类下的应用总数（不含状态筛选）。 */
        public final int totalCount;
        public final int blockedCount;
        public final int allowedCount;
        public final int visibleCount;
        public final boolean scanning;
        public final AppBlockingIntent.StatusFilter statusFilter;
        public final boolean hasAnyApps;
        public final String categoryFilter;
        public final String searchQuery;

        public UiState(List<BlockedApp> filteredApps,
                       int totalCount,
                       int blockedCount,
                       int allowedCount,
                       int visibleCount,
                       boolean scanning,
                       AppBlockingIntent.StatusFilter statusFilter,
                       boolean hasAnyApps,
                       String categoryFilter,
                       String searchQuery) {
            this.filteredApps = filteredApps;
            this.totalCount = totalCount;
            this.blockedCount = blockedCount;
            this.allowedCount = allowedCount;
            this.visibleCount = visibleCount;
            this.scanning = scanning;
            this.statusFilter = statusFilter;
            this.hasAnyApps = hasAnyApps;
            this.categoryFilter = categoryFilter;
            this.searchQuery = searchQuery;
        }
    }

    /** 一次性 UI 事件。 */
    public static final class UiEvent {
        public final String code;
        public final boolean isError;
        @Nullable public final String appName;
        public final int newCount;
        public final int updatedCount;
        public final int removedCount;

        public UiEvent(String code, boolean isError) {
            this(code, isError, null, 0, 0, 0);
        }

        public UiEvent(String code, boolean isError, @Nullable String appName) {
            this(code, isError, appName, 0, 0, 0);
        }

        public UiEvent(String code, boolean isError, @Nullable String appName,
                       int newCount, int updatedCount, int removedCount) {
            this.code = code;
            this.isError = isError;
            this.appName = appName;
            this.newCount = newCount;
            this.updatedCount = updatedCount;
            this.removedCount = removedCount;
        }
    }

    private final BlockedAppRepository repository;
    private final String userId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<String> toggleInFlight = Collections.synchronizedSet(new HashSet<>());
    private final AtomicInteger filterGeneration = new AtomicInteger();

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> categoryFilter = new MutableLiveData<>(AppCategory.FILTER_ALL);
    private final MutableLiveData<AppBlockingIntent.StatusFilter> statusFilter =
            new MutableLiveData<>(AppBlockingIntent.StatusFilter.ALL);
    private final MutableLiveData<Boolean> scanning = new MutableLiveData<>(false);
    private final SingleLiveEvent<UiEvent> uiEvent = new SingleLiveEvent<>();
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
            schedulePublishUiState();
        });
        uiState.addSource(searchQuery, q -> schedulePublishUiState());
        uiState.addSource(categoryFilter, c -> schedulePublishUiState());
        uiState.addSource(statusFilter, w -> schedulePublishUiState());
        uiState.addSource(scanning, s -> schedulePublishUiState());
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<UiEvent> getUiEvent() {
        return uiEvent;
    }

    @MainThread
    public void dispatch(@NonNull AppBlockingIntent intent) {
        switch (intent.type) {
            case SEARCH_DEBOUNCED:
                setSearchQueryDebounced(intent.text);
                break;
            case SEARCH_IMMEDIATE:
                setSearchQueryImmediate(intent.text);
                break;
            case SET_CATEGORY_FILTER:
                if (intent.text != null) {
                    categoryFilter.setValue(intent.text);
                }
                break;
            case SET_STATUS_FILTER:
                if (intent.statusFilter != null) {
                    statusFilter.setValue(intent.statusFilter);
                }
                break;
            case SCAN:
                scanInstalledApps();
                break;
            case TOGGLE:
                if (intent.app != null) {
                    updateBlockingStatus(intent.app, intent.blocked);
                }
                break;
            case CHECK_AUTO_SCAN:
                checkAutoScan();
                break;
            case RESET_FILTERS:
                resetFilters();
                break;
            default:
                break;
        }
    }

    private void setSearchQueryDebounced(String query) {
        pendingSearchText = query != null ? query.trim().toLowerCase() : "";
        if (pendingSearchPublish != null) {
            mainHandler.removeCallbacks(pendingSearchPublish);
        }
        pendingSearchPublish = () -> searchQuery.setValue(pendingSearchText);
        mainHandler.postDelayed(pendingSearchPublish, SEARCH_DEBOUNCE_MS);
    }

    private void setSearchQueryImmediate(String query) {
        if (pendingSearchPublish != null) {
            mainHandler.removeCallbacks(pendingSearchPublish);
            pendingSearchPublish = null;
        }
        searchQuery.setValue(query != null ? query.trim().toLowerCase() : "");
    }

    private void resetFilters() {
        if (pendingSearchPublish != null) {
            mainHandler.removeCallbacks(pendingSearchPublish);
            pendingSearchPublish = null;
        }
        searchQuery.setValue("");
        categoryFilter.setValue(AppCategory.FILTER_ALL);
        statusFilter.setValue(AppBlockingIntent.StatusFilter.ALL);
    }

    private void checkAutoScan() {
        AppExecutors.getInstance().diskIo(() -> {
            int count = repository.getAppCount(userId);
            if (count == 0) {
                scanInstalledApps();
            }
            // 有数据时不再弹 SCAN_HINT，扫描按钮已可见
        });
    }

    private void scanInstalledApps() {
        Runnable start = () -> {
            if (Boolean.TRUE.equals(scanning.getValue())) {
                postEvent(new UiEvent("SCAN_BUSY", false));
                return;
            }
            scanning.setValue(true);
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
                                result.updatedCategoryCount,
                                result.removedCount));
                    }
                } catch (Exception e) {
                    postEvent(new UiEvent("SCAN_FAILED", true));
                } finally {
                    mainHandler.post(() -> scanning.setValue(false));
                }
            });
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            start.run();
        } else {
            mainHandler.post(start);
        }
    }

    /**
     * 单一开关：isBlocked=true 表示专注时屏蔽；false 表示放行。
     */
    private void updateBlockingStatus(BlockedApp app, boolean isBlocked) {
        if (app == null || app.packageName == null) {
            return;
        }
        if (isCritical(app)) {
            return;
        }
        final String packageName = app.packageName;
        if (!toggleInFlight.add(packageName)) {
            return;
        }
        final String appName = app.appName != null ? app.appName : packageName;
        final boolean enabled = isBlocked;
        final boolean whitelisted = !isBlocked;

        AppExecutors.getInstance().diskIo(() -> {
            try {
                boolean ok = repository.updateBlockingFlags(userId, packageName, enabled, whitelisted);
                if (!ok) {
                    // CRITICAL 或找不到：静默，列表 LiveData 会回滚开关
                } else {
                    postEvent(new UiEvent(isBlocked ? "TOGGLE_BLOCKED" : "TOGGLE_ALLOWED", false, appName));
                }
            } finally {
                toggleInFlight.remove(packageName);
            }
        });
    }

    private boolean isCritical(BlockedApp app) {
        return BlockingRole.CRITICAL == BlockingRole.fromStorage(app.blockingRole)
                || repository.getPolicyEngine().isCritical(app.packageName);
    }

    private void postEvent(UiEvent event) {
        mainHandler.post(() -> uiEvent.setValue(event));
    }

    private void schedulePublishUiState() {
        final int gen = filterGeneration.incrementAndGet();
        final String query = searchQuery.getValue() != null ? searchQuery.getValue() : "";
        final String category = categoryFilter.getValue() != null
                ? categoryFilter.getValue() : AppCategory.FILTER_ALL;
        final AppBlockingIntent.StatusFilter status = statusFilter.getValue() != null
                ? statusFilter.getValue() : AppBlockingIntent.StatusFilter.ALL;
        final boolean isScanning = Boolean.TRUE.equals(scanning.getValue());
        final List<BlockedApp> snapshot = new ArrayList<>(allApps);
        final boolean hasAny = !snapshot.isEmpty();

        AppExecutors.getInstance().diskIo(() -> {
            List<BlockedApp> afterSearchCategory = new ArrayList<>();
            int blockedCount = 0;
            int allowedCount = 0;

            for (BlockedApp app : snapshot) {
                boolean matchesSearch = query.isEmpty()
                        || (app.appName != null && app.appName.toLowerCase().contains(query))
                        || (app.packageName != null && app.packageName.toLowerCase().contains(query));
                boolean matchesCategory = AppCategory.FILTER_ALL.equals(category)
                        || category.equals(app.category);
                if (!matchesSearch || !matchesCategory) {
                    continue;
                }
                afterSearchCategory.add(app);
                if (app.isEnabled) {
                    blockedCount++;
                }
                if (app.isWhitelisted) {
                    allowedCount++;
                }
            }

            List<BlockedApp> filtered = new ArrayList<>();
            for (BlockedApp app : afterSearchCategory) {
                boolean matchesStatus;
                switch (status) {
                    case BLOCKED:
                        matchesStatus = app.isEnabled;
                        break;
                    case ALLOWED:
                        matchesStatus = app.isWhitelisted;
                        break;
                    case ALL:
                    default:
                        matchesStatus = true;
                        break;
                }
                if (matchesStatus) {
                    filtered.add(app);
                }
            }

            final int totalScoped = afterSearchCategory.size();
            final int blockedScoped = blockedCount;
            final int allowedScoped = allowedCount;
            final List<BlockedApp> result = Collections.unmodifiableList(filtered);

            mainHandler.post(() -> {
                if (gen != filterGeneration.get()) {
                    return;
                }
                uiState.setValue(new UiState(
                        result,
                        totalScoped,
                        blockedScoped,
                        allowedScoped,
                        result.size(),
                        isScanning,
                        status,
                        hasAny,
                        category,
                        query
                ));
            });
        });
    }

    @Override
    protected void onCleared() {
        if (pendingSearchPublish != null) {
            mainHandler.removeCallbacks(pendingSearchPublish);
        }
        filterGeneration.incrementAndGet();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
