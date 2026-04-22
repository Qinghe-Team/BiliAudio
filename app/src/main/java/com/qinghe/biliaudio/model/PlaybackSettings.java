package com.qinghe.biliaudio.model;

public class PlaybackSettings {
    private final String currentTitle;
    private final double playbackSpeed;
    private final int sleepTimerMinutes;
    private final boolean playing;

    public PlaybackSettings(String currentTitle, double playbackSpeed, int sleepTimerMinutes, boolean playing) {
        this.currentTitle = currentTitle;
        this.playbackSpeed = playbackSpeed;
        this.sleepTimerMinutes = sleepTimerMinutes;
        this.playing = playing;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public double getPlaybackSpeed() {
        return playbackSpeed;
    }

    public int getSleepTimerMinutes() {
        return sleepTimerMinutes;
    }

    public boolean isPlaying() {
        return playing;
    }
}
