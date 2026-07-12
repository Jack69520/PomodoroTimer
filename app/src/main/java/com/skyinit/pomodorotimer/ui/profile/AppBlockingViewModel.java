package com.skyinit.pomodorotimer.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.repository.BlockedAppRepository;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用屏蔽管理页 ViewModel：列表观察、筛选与扫描同步均由 Repository 驱动。
 */
public class AppBlockingViewModel extends ViewModel {

    public static final class UiState {
        public final List<BlockedApp> filteredApps;
        public final int totalCount;
        public final int blockedCount;
        public final int whitelistCount;
        public final boolean scanning;
        public final boolean showWhitelistOnly;
        public final String message;
        public final boolean messageIsError;

        public UiState(List<BlockedApp> filteredApps,
                         int totalCount,
                         int blockedCount,
                         int whitelistCount,
                         boolean scanning,
                         boolean showWhitelistOnly,
                         String message,
                         boolean messageIsError) {
            this.filteredApps = filteredApps;
            this.totalCount = totalCount;
            this.blockedCount = blockedCount;
            this.whitelistCount = whitelistCount;
            this.scanning = scanning;
            this.showWhitelistOnly = showWhitelistOnly;
            this.message = message;
            this.messageIsError = messageIsError;
        }
    }

    private final BlockedAppRepository repository;
    private final String userId;

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> categoryFilter = new MutableLiveData<>(AppCategory.FILTER_ALL);
    private final MutableLiveData<Boolean> showWhitelistOnly = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> scanning = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> toastIsError = new MutableLiveData<>(false);
    private final MediatorLiveData<UiState> uiState = new MediatorLiveData<>();

    private List<BlockedApp> allApps = new ArrayList<>();

    public AppBlockingViewModel(BlockedAppRepository repository, String userId) {
        this.repository = repository;
        this.userId = userId;

        LiveData<List<BlockedApp>> source = repository.observeApps(userId);
        uiState.addSource(source, apps -> {
            allApps = apps != null ? apps : new ArrayList<>();
            publishUiState();
        });
        uiState.addSource(searchQuery, q -> publishUiState());
        uiState.addSource(categoryFilter, c -> publishUiState());
        uiState.addSource(showWhitelistOnly, w -> publishUiState());
        uiState.addSource(scanning, s -> publishUiState());
        uiState.addSource(toastMessage, m -> publishUiState());
        uiState.addSource(toastIsError, e -> publishUiState());
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query.toLowerCase() : "");
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
                toastIsError.postValue(false);
                toastMessage.postValue("SCAN_HINT");
            }
        });
    }

    public void scanInstalledApps() {
        scanning.postValue(true);
        AppExecutors.getInstance().diskIo(() -> {
            try {
                BlockedAppRepository.ScanResult result = repository.scanAndSync(userId);
                toastIsError.postValue(false);
                if (result.newCount == 0 && result.updatedCategoryCount == 0 && result.removedCount == 0) {
                    toastMessage.postValue("SCAN_NO_CHANGE");
                } else {
                    toastMessage.postValue("SCAN_COMPLETE:" + result.newCount + ":" + result.updatedCategoryCount);
                }
            } catch (Exception e) {
                toastIsError.postValue(true);
                toastMessage.postValue("SCAN_FAILED");
            } finally {
                scanning.postValue(false);
            }
        });
    }

    public void updateBlockingStatus(BlockedApp app, boolean isBlocked) {
        app.isEnabled = isBlocked;
        app.isWhitelisted = !isBlocked;
        AppExecutors.getInstance().diskIo(() -> repository.updateAppAndNotifyService(app));
    }

    public void updateWhitelistStatus(BlockedApp app, boolean isWhitelisted) {
        app.isWhitelisted = isWhitelisted;
        app.isEnabled = !isWhitelisted;
        AppExecutors.getInstance().diskIo(() -> repository.updateAppAndNotifyService(app));
    }

    public void clearToast() {
        toastMessage.setValue(null);
    }

    private void publishUiState() {
        String query = searchQuery.getValue() != null ? searchQuery.getValue() : "";
        String category = categoryFilter.getValue() != null ? categoryFilter.getValue() : AppCategory.FILTER_ALL;
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
                    || app.appName.toLowerCase().contains(query)
                    || app.packageName.toLowerCase().contains(query);
            boolean matchesCategory = AppCategory.FILTER_ALL.equals(category)
                    || category.equals(app.category);
            boolean matchesWhitelist = !whitelistOnly || app.isWhitelisted;

            if (matchesSearch && matchesCategory && matchesWhitelist) {
                filtered.add(app);
            }
        }

        uiState.setValue(new UiState(
                filtered,
                allApps.size(),
                blockedCount,
                whitelistCount,
                isScanning,
                whitelistOnly,
                toastMessage.getValue(),
                Boolean.TRUE.equals(toastIsError.getValue())
        ));
    }
}
