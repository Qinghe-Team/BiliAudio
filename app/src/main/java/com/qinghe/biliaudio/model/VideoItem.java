package com.qinghe.biliaudio.model;

public class VideoItem {
    private final String id;
    private final String title;
    private final String author;
    private final String summary;
    private final String durationLabel;
    private final String playUrl;

    public VideoItem(String id, String title, String author, String summary, String durationLabel, String playUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.summary = summary;
        this.durationLabel = durationLabel;
        this.playUrl = playUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSummary() {
        return summary;
    }

    public String getDurationLabel() {
        return durationLabel;
    }

    public String getPlayUrl() {
        return playUrl;
    }
}
