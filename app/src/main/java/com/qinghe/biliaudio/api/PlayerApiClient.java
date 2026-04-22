package com.qinghe.biliaudio.api;

import java.util.Arrays;
import java.util.List;

public class PlayerApiClient {
    public List<Double> getSpeedPresets() {
        return Arrays.asList(0.5, 1.0, 1.5, 2.0);
    }

    public List<Integer> getSleepTimerPresets() {
        return Arrays.asList(10, 20, 30, 60);
    }
}
