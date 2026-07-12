package com.skyinit.pomodorotimer.ui.bootstrap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.data.repository.AppInitializationRepository;

/**
 * 主界面壳启动引导 ViewModel：观察应用数据层初始化状态，避免在账户就绪前绑定业务 Repository。
 */
public final class AppBootstrapViewModel extends ViewModel {

    private final App app;
    private final AppInitializationRepository initializationRepository;

    public AppBootstrapViewModel(App app, AppInitializationRepository initializationRepository) {
        this.app = app;
        this.initializationRepository = initializationRepository;
    }

    public LiveData<AppInitializationRepository.State> getInitializationState() {
        return initializationRepository.getState();
    }

    public LiveData<String> getInitializationError() {
        return initializationRepository.getErrorMessage();
    }

    public boolean isUserDataInitialized() {
        return app.isUserDataInitialized();
    }

    /** 幂等触发异步初始化；若已完成则立即处于 READY。 */
    public void ensureInitialized() {
        app.initializeAfterConsent(null);
    }

    public static ViewModelProvider.Factory factory(@NonNull App app) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(AppBootstrapViewModel.class)) {
                    return (T) new AppBootstrapViewModel(app, app.getInitializationRepository());
                }
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
        };
    }
}
