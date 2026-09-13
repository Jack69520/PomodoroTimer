package com.skyinit.pomodorotimer.ui.profile;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.data.repository.BlockedAppRepository;
import com.skyinit.pomodorotimer.util.AppExecutors;
import com.skyinit.pomodorotimer.util.SingleLiveEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用分类编辑页 ViewModel。
 */
public class AppCategoryEditViewModel extends ViewModel {

    public static final class UiState {
        public final boolean loading;
        public final boolean saving;
        @Nullable public final BlockedApp app;
        @Nullable public final String selectedCategory;

        public UiState(boolean loading,
                       boolean saving,
                       @Nullable BlockedApp app,
                       @Nullable String selectedCategory) {
            this.loading = loading;
            this.saving = saving;
            this.app = app;
            this.selectedCategory = selectedCategory;
        }

        public static UiState loading() {
            return new UiState(true, false, null, null);
        }
    }

    public static final class Effect {
        public final String code;
        public final boolean finishOk;

        public Effect(String code, boolean finishOk) {
            this.code = code;
            this.finishOk = finishOk;
        }
    }

    private final BlockedAppRepository repository;
    private final String userId;
    private final String packageName;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean saveInFlight = new AtomicBoolean(false);

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<Effect> effects = new SingleLiveEvent<>();

    public AppCategoryEditViewModel(BlockedAppRepository repository,
                                    String userId,
                                    String packageName) {
        this.repository = repository;
        this.userId = userId;
        this.packageName = packageName;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<Effect> getEffects() {
        return effects;
    }

    @MainThread
    public void load() {
        uiState.setValue(UiState.loading());
        AppExecutors.getInstance().diskIo(() -> {
            try {
                BlockedApp app = repository.getAppByPackage(userId, packageName);
                mainHandler.post(() -> {
                    if (app == null) {
                        effects.setValue(new Effect("NOT_FOUND", true));
                    } else {
                        uiState.setValue(new UiState(false, false, app, app.category));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> effects.setValue(new Effect("LOAD_FAILED", true)));
            }
        });
    }

    @MainThread
    public void selectCategory(@Nullable String category) {
        UiState current = uiState.getValue();
        if (current == null || current.app == null || current.loading) {
            return;
        }
        uiState.setValue(new UiState(false, current.saving, current.app, category));
    }

    @MainThread
    public void saveManual() {
        UiState current = uiState.getValue();
        if (current == null || current.app == null || current.selectedCategory == null) {
            return;
        }
        if (current.selectedCategory.equals(current.app.category) && current.app.categoryManual) {
            effects.setValue(new Effect("SAVED", true));
            return;
        }
        if (!saveInFlight.compareAndSet(false, true)) {
            return;
        }
        uiState.setValue(new UiState(false, true, current.app, current.selectedCategory));
        final String category = current.selectedCategory;
        AppExecutors.getInstance().diskIo(() -> {
            try {
                boolean ok = repository.updateManualCategory(userId, packageName, category);
                mainHandler.post(() -> {
                    saveInFlight.set(false);
                    if (ok) {
                        effects.setValue(new Effect("SAVED", true));
                    } else {
                        UiState latest = uiState.getValue();
                        uiState.setValue(new UiState(false, false,
                                latest != null ? latest.app : current.app,
                                category));
                        effects.setValue(new Effect("SAVE_FAILED", false));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    saveInFlight.set(false);
                    UiState latest = uiState.getValue();
                    uiState.setValue(new UiState(false, false,
                            latest != null ? latest.app : current.app,
                            category));
                    effects.setValue(new Effect("SAVE_FAILED", false));
                });
            }
        });
    }

    @MainThread
    public void restoreAuto() {
        UiState current = uiState.getValue();
        if (current == null || current.app == null || !current.app.categoryManual) {
            return;
        }
        if (!saveInFlight.compareAndSet(false, true)) {
            return;
        }
        uiState.setValue(new UiState(false, true, current.app, current.selectedCategory));
        AppExecutors.getInstance().diskIo(() -> {
            try {
                String restored = repository.restoreAutoCategory(userId, packageName);
                mainHandler.post(() -> {
                    saveInFlight.set(false);
                    if (restored != null) {
                        effects.setValue(new Effect("RESTORED", true));
                    } else {
                        UiState latest = uiState.getValue();
                        uiState.setValue(new UiState(false, false,
                                latest != null ? latest.app : current.app,
                                latest != null ? latest.selectedCategory : current.selectedCategory));
                        effects.setValue(new Effect("RESTORE_FAILED", false));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    saveInFlight.set(false);
                    UiState latest = uiState.getValue();
                    uiState.setValue(new UiState(false, false,
                            latest != null ? latest.app : current.app,
                            latest != null ? latest.selectedCategory : current.selectedCategory));
                    effects.setValue(new Effect("RESTORE_FAILED", false));
                });
            }
        });
    }

    @Override
    protected void onCleared() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
