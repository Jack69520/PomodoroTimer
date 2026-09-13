package com.skyinit.pomodorotimer.ui.settings;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.ui.theme.WallpaperCatalog;
import com.skyinit.pomodorotimer.ui.theme.WallpaperThemeRepository;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 主题色选择页 ViewModel：分类 Tab 过滤 + 防连点写盘（MVI）。
 */
public class ThemeColorViewModel extends ViewModel {

    private final Application application;
    private final WallpaperThemeRepository repository;
    private final AppExecutors executors = AppExecutors.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<ThemeColorUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<ThemeColorEffect> effects = new SingleLiveEvent<>();

    private final AtomicBoolean applying = new AtomicBoolean(false);
    private final AtomicInteger applyGeneration = new AtomicInteger(0);

    @NonNull
    private WallpaperCatalog.Category activeCategory = WallpaperCatalog.Category.STANDARD;

    public ThemeColorViewModel(@NonNull Application application,
                               @NonNull WallpaperThemeRepository repository) {
        this.application = application;
        this.repository = repository;
        dispatch(ThemeColorIntent.refresh());
    }

    /** Backward-compatible factory wiring that still receives SettingsManager via container. */
    public ThemeColorViewModel(@NonNull Application application,
                               @NonNull com.skyinit.pomodorotimer.data.repository.SettingsManager settingsManager) {
        this(application, new WallpaperThemeRepository(application, settingsManager));
    }

    @NonNull
    public LiveData<ThemeColorUiState> getUiState() {
        return uiState;
    }

    @NonNull
    public LiveData<ThemeColorEffect> getEffects() {
        return effects;
    }

    @MainThread
    public void dispatch(@NonNull ThemeColorIntent intent) {
        switch (intent.type) {
            case REFRESH: {
                String selected = repository.getSelectedKey();
                activeCategory = resolveCategory(selected);
                publishList(selected, applying.get());
                break;
            }
            case SELECT_CATEGORY:
                if (intent.category != null && intent.category != activeCategory) {
                    activeCategory = intent.category;
                    ThemeColorUiState current = uiState.getValue();
                    String selected = current != null
                            ? current.selectedKey
                            : repository.getSelectedKey();
                    publishList(selected, applying.get());
                }
                break;
            case SELECT_THEME:
                if (intent.themeKey != null) {
                    selectTheme(intent.themeKey);
                }
                break;
            default:
                break;
        }
    }

    private void selectTheme(@NonNull String themeKey) {
        if (themeKey.isEmpty()) {
            return;
        }
        ThemeColorUiState current = uiState.getValue();
        if (current != null && themeKey.equals(current.selectedKey)) {
            return;
        }
        if (!applying.compareAndSet(false, true)) {
            return;
        }
        int generation = applyGeneration.incrementAndGet();
        publishList(themeKey, true);

        executors.diskIo(() -> {
            try {
                WallpaperCatalog.WallpaperOption saved = repository.setSelectedKey(themeKey);
                if (generation != applyGeneration.get()) {
                    return;
                }
                mainHandler.post(() -> {
                    if (generation != applyGeneration.get()) {
                        return;
                    }
                    applying.set(false);
                    publishList(saved.key, false);
                    effects.setValue(ThemeColorEffect.themeApplied(saved.key));
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    applying.set(false);
                    publishList(repository.getSelectedKey(), false);
                    effects.setValue(ThemeColorEffect.themeFailed());
                });
            }
        });
    }

    private void publishList(@NonNull String selectedKey, boolean isApplying) {
        List<WallpaperCatalog.WallpaperOption> options = WallpaperCatalog.allOptions(application);
        List<ThemeColorUiState.Item> items = new ArrayList<>();
        for (WallpaperCatalog.WallpaperOption option : options) {
            if (option.category != activeCategory) {
                continue;
            }
            items.add(ThemeColorUiState.Item.option(option, option.key.equals(selectedKey)));
        }
        uiState.setValue(new ThemeColorUiState(selectedKey, activeCategory, isApplying, items));
    }

    @NonNull
    private WallpaperCatalog.Category resolveCategory(@NonNull String selectedKey) {
        WallpaperCatalog.WallpaperOption option =
                WallpaperCatalog.findByKey(application, selectedKey);
        return option != null ? option.category : WallpaperCatalog.Category.STANDARD;
    }
}
