package com.skyinit.pomodorotimer.data.repository;



import androidx.lifecycle.LiveData;

import androidx.lifecycle.MutableLiveData;



import com.skyinit.pomodorotimer.data.model.TimerUiState;

import com.skyinit.pomodorotimer.service.TimerService;



/**

 * 计时器运行态的唯一数据源（Service → Repository → ViewModel → UI）。

 */

public class TimerStateRepository {



    private final MutableLiveData<TimerUiState> state;



    public TimerStateRepository(TimerSettingsRepository timerSettings) {
        // 构造阶段不依赖活跃账户；用户设置加载后由 HomeViewModel 同步
        long defaultMs = TimerSettingsRepository.getFactoryDefaultMs();
        state = new MutableLiveData<>(TimerUiState.idle(defaultMs));
    }



    public LiveData<TimerUiState> getState() {

        return state;

    }



    public TimerUiState getCurrentState() {

        return state.getValue();

    }



    public void publish(long timeLeftMillis, boolean running, boolean paused, int sessionType,

                        boolean awaitingPostBreakChoice, boolean longBreak) {

        state.postValue(new TimerUiState(

                timeLeftMillis, running, paused, sessionType, awaitingPostBreakChoice, longBreak));

    }



    public void syncFromService(TimerService service) {

        if (service == null) {

            return;

        }

        publish(

                service.getTimeLeft(),

                service.isRunning(),

                service.isPaused(),

                service.getSessionType(),

                service.isAwaitingPostBreakChoice(),

                service.isLongBreak()

        );

    }

}

