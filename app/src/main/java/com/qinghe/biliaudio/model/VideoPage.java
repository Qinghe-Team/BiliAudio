package com.qinghe.biliaudio.model;

/** Represents a single part (分P) of a Bilibili video. */
public class VideoPage {
    private final int page;
    private final long cid;
    private final String title;
    private final int durationSeconds;

    public VideoPage(int page, long cid, String title, int durationSeconds) {
        this.page = page;
        this.cid = cid;
        this.title = title;
        this.durationSeconds = durationSeconds;
    }

    public int getPage() { return page; }
    public long getCid() { return cid; }
    public String getTitle() { return title; }
    public int getDurationSeconds() { return durationSeconds; }

    public String displayTitle() {
        return "P" + page + (title.isEmpty() ? "" : " " + title);
    }
}
