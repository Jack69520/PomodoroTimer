package com.skyinit.pomodorotimer.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 应用冷启动初始化状态（MVVM 可观察层）。
 * <p>
 * UI 通过 {@link com.skyinit.pomodorotimer.ui.bootstrap.AppBootstrapViewModel} 观察本仓库，
 * 在 {@link com.skyinit.pomodorotimer.App#initializeAfterConsent(Runnable)} 完成前避免访问依赖账户的数据层。
 */
public final class AppInitializationRepository {

    public enum State {
        IDLE,
        LOADING,
        READY,
        FAILED
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public State getCurrentState() {
        State current = state.getValue();
        return current != null ? current : State.IDLE;
    }

    public void markLoading() {
        state.postValue(State.LOADING);
        errorMessage.postValue(null);
    }

    public void markReady() {
        state.postValue(State.READY);
        errorMessage.postValue(null);
    }

    public void markFailed(String message) {
        state.postValue(State.FAILED);
        errorMessage.postValue(message);
    }
}
