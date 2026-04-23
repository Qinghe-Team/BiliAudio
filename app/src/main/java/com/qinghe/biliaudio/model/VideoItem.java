package com.qinghe.biliaudio.model;

import java.util.Collections;
import java.util.List;

public class VideoItem {
    private final String id;
    private final String title;
    private final String author;
    private final String summary;
    private final String durationLabel;
    private final String playUrl;
    private final long cid;
    private final long avid;
    private final String coverUrl;
    private final List<VideoPage> pages;

    public VideoItem(String id, String title, String author, String summary, String durationLabel, String playUrl) {
        this(id, title, author, summary, durationLabel, playUrl, 0L, 0L, "", Collections.emptyList());
    }

    public VideoItem(String id, String title, String author, String summary, String durationLabel, String playUrl, long cid, long avid) {
        this(id, title, author, summary, durationLabel, playUrl, cid, avid, "", Collections.emptyList());
    }

    public VideoItem(String id, String title, String author, String summary, String durationLabel,
                     String playUrl, long cid, long avid, String coverUrl, List<VideoPage> pages) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.summary = summary;
        this.durationLabel = durationLabel;
        this.playUrl = playUrl;
        this.cid = cid;
        this.avid = avid;
        this.coverUrl = coverUrl != null ? coverUrl : "";
        this.pages = pages != null ? pages : Collections.emptyList();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSummary() { return summary; }
    public String getDurationLabel() { return durationLabel; }
    public String getPlayUrl() { return playUrl; }
    public long getCid() { return cid; }
    public long getAvid() { return avid; }
    public String getCoverUrl() { return coverUrl; }
    public List<VideoPage> getPages() { return pages; }

    public boolean hasMultipleParts() { return pages.size() > 1; }

    /** Returns a copy with the given stream URL filled in. */
    public VideoItem withPlayUrl(String url) {
        return new VideoItem(id, title, author, summary, durationLabel, url, cid, avid, coverUrl, pages);
    }

    /** Returns a copy with cid and avid filled in. */
    public VideoItem withCid(long newCid, long newAvid) {
        return new VideoItem(id, title, author, summary, durationLabel, playUrl, newCid, newAvid, coverUrl, pages);
    }

    /** Returns a copy with a specific part selected (updates cid). */
    public VideoItem withPage(VideoPage page) {
        return new VideoItem(id, title, author, summary, durationLabel, playUrl,
                page.getCid(), avid, coverUrl, pages);
    }
}
