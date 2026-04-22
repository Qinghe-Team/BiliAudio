package com.qinghe.biliaudio.api;

import com.qinghe.biliaudio.model.VideoItem;

import java.util.List;

public class VideoApiClient {
    public List<VideoItem> getFeaturedVideos() {
        return DemoCatalog.videos();
    }

    public VideoItem getVideoDetail(String videoId) {
        for (VideoItem item : DemoCatalog.videos()) {
            if (item.getId().equals(videoId)) {
                return item;
            }
        }
        return DemoCatalog.videos().get(0);
    }
}
