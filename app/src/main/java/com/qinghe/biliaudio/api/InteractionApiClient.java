package com.qinghe.biliaudio.api;

import com.qinghe.biliaudio.model.InteractionSummary;

public class InteractionApiClient {
    public InteractionSummary getSummary(String videoId) {
        return new InteractionSummary(128, 12, 56, 23);
    }
}
