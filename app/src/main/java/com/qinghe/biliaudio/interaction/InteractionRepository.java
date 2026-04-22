package com.qinghe.biliaudio.interaction;

import com.qinghe.biliaudio.api.InteractionApiClient;
import com.qinghe.biliaudio.model.InteractionSummary;

public class InteractionRepository {
    private final InteractionApiClient interactionApiClient;
    private InteractionSummary currentSummary;

    public InteractionRepository(InteractionApiClient interactionApiClient) {
        this.interactionApiClient = interactionApiClient;
        currentSummary = interactionApiClient.getSummary("default");
    }

    public InteractionSummary getSummary(String videoId) {
        currentSummary = interactionApiClient.getSummary(videoId);
        return currentSummary;
    }

    public InteractionSummary like() {
        currentSummary = new InteractionSummary(
                currentSummary.getLikes() + 1,
                currentSummary.getCoins(),
                currentSummary.getFavorites(),
                currentSummary.getComments()
        );
        return currentSummary;
    }

    public InteractionSummary coin() {
        currentSummary = new InteractionSummary(
                currentSummary.getLikes(),
                currentSummary.getCoins() + 1,
                currentSummary.getFavorites(),
                currentSummary.getComments()
        );
        return currentSummary;
    }

    public InteractionSummary favorite() {
        currentSummary = new InteractionSummary(
                currentSummary.getLikes(),
                currentSummary.getCoins(),
                currentSummary.getFavorites() + 1,
                currentSummary.getComments()
        );
        return currentSummary;
    }
}
