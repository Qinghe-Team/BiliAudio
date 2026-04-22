package com.qinghe.biliaudio.player;

import com.qinghe.biliaudio.api.PlayerApiClient;
import com.qinghe.biliaudio.model.PlaybackSettings;
import com.qinghe.biliaudio.model.VideoItem;

import java.util.List;

public class PlayerController {
    private final PlayerApiClient playerApiClient;
    private PlaybackSettings playbackSettings;

    public PlayerController(PlayerApiClient playerApiClient) {
        this.playerApiClient = playerApiClient;
        playbackSettings = new PlaybackSettings("未开始播放", 1.0, 0, false);
    }

    public List<Double> getSpeedPresets() {
        return playerApiClient.getSpeedPresets();
    }

    public List<Integer> getSleepTimerPresets() {
        return playerApiClient.getSleepTimerPresets();
    }

    public PlaybackSettings getPlaybackSettings() {
        return playbackSettings;
    }

    public PlaybackSettings play(VideoItem videoItem) {
        playbackSettings = new PlaybackSettings(videoItem.getTitle(), playbackSettings.getPlaybackSpeed(), playbackSettings.getSleepTimerMinutes(), true);
        return playbackSettings;
    }

    public PlaybackSettings setPlaybackSpeed(double speed) {
        double normalized = Math.max(0.5, Math.min(3.0, speed));
        playbackSettings = new PlaybackSettings(playbackSettings.getCurrentTitle(), normalized, playbackSettings.getSleepTimerMinutes(), playbackSettings.isPlaying());
        return playbackSettings;
    }

    public PlaybackSettings setSleepTimerMinutes(int minutes) {
        int normalized = Math.max(0, Math.min(180, minutes));
        playbackSettings = new PlaybackSettings(playbackSettings.getCurrentTitle(), playbackSettings.getPlaybackSpeed(), normalized, playbackSettings.isPlaying());
        return playbackSettings;
    }
}
