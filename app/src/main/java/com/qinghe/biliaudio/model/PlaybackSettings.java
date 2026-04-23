package com.qinghe.biliaudio.model;

public class PlaybackSettings {
    private final String currentTitle;
    private final double playbackSpeed;
    private final int sleepTimerMinutes;
    private final boolean playing;
    private final long positionMs;
    private final long durationMs;
    private final float volume;

    public PlaybackSettings(String currentTitle, double playbackSpeed, int sleepTimerMinutes, boolean playing) {
        this(currentTitle, playbackSpeed, sleepTimerMinutes, playing, 0L, 0L, 1.0f);
    }

    public PlaybackSettings(String currentTitle, double playbackSpeed, int sleepTimerMinutes,
                            boolean playing, long positionMs, long durationMs, float volume) {
        this.currentTitle = currentTitle;
        this.playbackSpeed = playbackSpeed;
        this.sleepTimerMinutes = sleepTimerMinutes;
        this.playing = playing;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.volume = volume;
    }

    public String getCurrentTitle() { return currentTitle; }
    public double getPlaybackSpeed() { return playbackSpeed; }
    public int getSleepTimerMinutes() { return sleepTimerMinutes; }
    public boolean isPlaying() { return playing; }
    public long getPositionMs() { return positionMs; }
    public long getDurationMs() { return durationMs; }
    public float getVolume() { return volume; }

    /** Progress 0.0–1.0 suitable for slider display. */
    public float getProgress() {
        return durationMs > 0 ? (float) positionMs / durationMs : 0f;
    }
}
