package com.qinghe.biliaudio.video;

import com.qinghe.biliaudio.api.VideoApiClient;
import com.qinghe.biliaudio.model.VideoItem;

import java.util.List;

public class VideoRepository {
    private final VideoApiClient videoApiClient;

    public VideoRepository(VideoApiClient videoApiClient) {
        this.videoApiClient = videoApiClient;
    }

    public List<VideoItem> getFeaturedVideos() {
        return videoApiClient.getFeaturedVideos();
    }

    public VideoItem getVideoDetail(String videoId) {
        return videoApiClient.getVideoDetail(videoId);
    }
}
