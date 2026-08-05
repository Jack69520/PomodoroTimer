package com.skyinit.pomodorotimer.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.skyinit.pomodorotimer.data.dao.BlockedAppDao;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.domain.appidentity.AppIdentityRulesLoader;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyEngine;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyRulesLoader;
import com.skyinit.pomodorotimer.util.AppBlockingServiceUtils;
import com.skyinit.pomodorotimer.util.AppCategoryRulesLoader;
import com.skyinit.pomodorotimer.util.AppScanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 屏蔽应用数据仓库：封装扫描入库、策略应用与 Room 持久化。
 */
public final class BlockedAppRepository {

    public static final class ScanResult {
        public final int newCount;
        public final int updatedCategoryCount;
        public final int removedCount;
        public final boolean busy;

        public ScanResult(int newCount, int updatedCategoryCount, int removedCount) {
            this(newCount, updatedCategoryCount, removedCount, false);
        }

        public ScanResult(int newCount, int updatedCategoryCount, int removedCount, boolean busy) {
            this.newCount = newCount;
            this.updatedCategoryCount = updatedCategoryCount;
            this.removedCount = removedCount;
            this.busy = busy;
        }
    }

    private final Context appContext;
    private final BlockedAppDao blockedAppDao;
    private final AccountManager accountManager;
    private final AtomicBoolean scanInFlight = new AtomicBoolean(false);

    public BlockedAppRepository(Context context,
                                BlockedAppDao blockedAppDao,
                                AccountManager accountManager) {
        this.appContext = context.getApplicationContext();
        this.blockedAppDao = blockedAppDao;
        this.accountManager = accountManager;
    }

    public LiveData<List<BlockedApp>> observeApps(String userId) {
        return blockedAppDao.getAllBlockedApps(userId);
    }

    public int getAppCount(String userId) {
        return blockedAppDao.getAppCount(userId);
    }

    public BlockedApp getAppByPackage(String userId, String packageName) {
        return blockedAppDao.getBlockedAppByPackage(userId, packageName);
    }

    public void updateApp(BlockedApp app) {
        blockedAppDao.update(app);
    }

    public void updateAppAndNotifyService(BlockedApp app) {
        blockedAppDao.update(app);
        AppBlockingServiceUtils.notifyCacheRefresh(appContext);
    }

    /**
     * 按包名更新屏蔽/白名单状态；CRITICAL 角色拒绝修改。
     * @return true 表示已写入；false 表示拒绝或找不到应用
     */
    public boolean updateBlockingFlags(String userId,
                                       String packageName,
                                       boolean isEnabled,
                                       boolean isWhitelisted) {
        BlockingPolicyEngine policy = getPolicyEngine();
        if (policy.isCritical(packageName)) {
            return false;
        }
        BlockedApp existing = blockedAppDao.getBlockedAppByPackage(userId, packageName);
        if (existing == null) {
            return false;
        }
        existing.isEnabled = isEnabled;
        existing.isWhitelisted = isWhitelisted;
        // 互斥防呆
        if (existing.isWhitelisted) {
            existing.isEnabled = false;
        } else if (existing.isEnabled) {
            existing.isWhitelisted = false;
        }
        updateAppAndNotifyService(existing);
        return true;
    }

    public ScanResult scanAndSync(String userId) {
        if (!scanInFlight.compareAndSet(false, true)) {
            return new ScanResult(0, 0, 0, true);
        }
        try {
            int removedCount = cleanUninstalledApps(userId);

            AppIdentityRulesLoader.reload(appContext);
            AppCategoryRulesLoader.reload(appContext);
            BlockingPolicyRulesLoader.reload(appContext);
            BlockingPolicyEngine policy = BlockingPolicyRulesLoader.getInstance().createEngine();

            AppScanner scanner = new AppScanner(appContext, policy);
            List<BlockedApp> scannedApps = scanner.scanInstalledApps();
            policy.applyWhitelistConfig(scannedApps);

            List<BlockedApp> newApps = new ArrayList<>();
            int updatedCount = 0;
            for (BlockedApp scannedApp : scannedApps) {
                scannedApp.userId = userId;
                if (!blockedAppDao.appExists(userId, scannedApp.packageName)) {
                    newApps.add(scannedApp);
                    continue;
                }

                BlockedApp existing = blockedAppDao.getBlockedAppByPackage(userId, scannedApp.packageName);
                if (existing == null) {
                    continue;
                }

                boolean changed = false;
                if (!scannedApp.appName.equals(existing.appName)) {
                    existing.appName = scannedApp.appName;
                    changed = true;
                }
                if (!existing.categoryManual && !scannedApp.category.equals(existing.category)) {
                    existing.category = scannedApp.category;
                    changed = true;
                    updatedCount++;
                }
                // 身份与角色以策略为准，复扫始终刷新
                if (scannedApp.provenance != null
                        && !scannedApp.provenance.equals(existing.provenance)) {
                    existing.provenance = scannedApp.provenance;
                    changed = true;
                }
                if (scannedApp.blockingRole != null
                        && !scannedApp.blockingRole.equals(existing.blockingRole)) {
                    existing.blockingRole = scannedApp.blockingRole;
                    changed = true;
                }
                if (changed) {
                    blockedAppDao.update(existing);
                }
            }

            if (!newApps.isEmpty()) {
                blockedAppDao.insertAll(newApps);
            }

            return new ScanResult(newApps.size(), updatedCount, removedCount);
        } finally {
            scanInFlight.set(false);
        }
    }

    public BlockedApp buildBlockedAppForRuntime(String packageName) {
        BlockingPolicyEngine policy = BlockingPolicyRulesLoader.getInstance().createEngine();
        return AppScanner.buildBlockedApp(appContext, packageName, policy);
    }

    public BlockingPolicyEngine getPolicyEngine() {
        return BlockingPolicyRulesLoader.getInstance().createEngine();
    }

    private int cleanUninstalledApps(String userId) {
        AppScanner scanner = new AppScanner(appContext, getPolicyEngine());
        List<BlockedApp> installedApps = scanner.scanInstalledApps();
        Set<String> installedPackageNames = new HashSet<>();
        for (BlockedApp app : installedApps) {
            installedPackageNames.add(app.packageName);
        }

        List<BlockedApp> allDbApps = blockedAppDao.getAllAppsSync(userId);
        int removed = 0;
        for (BlockedApp dbApp : allDbApps) {
            if (!installedPackageNames.contains(dbApp.packageName)) {
                blockedAppDao.delete(dbApp);
                removed++;
            }
        }
        return removed;
    }
}
