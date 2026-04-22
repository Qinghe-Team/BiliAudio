package com.qinghe.biliaudio.api;

import com.qinghe.biliaudio.model.VideoItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SearchApiClient {
    private final VideoApiClient videoApiClient;

    public SearchApiClient(VideoApiClient videoApiClient) {
        this.videoApiClient = videoApiClient;
    }

    public List<String> getHotKeywords() {
        return Arrays.asList("广播剧", "科幻", "儿童故事", "纪录片");
    }

    public List<VideoItem> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return videoApiClient.getFeaturedVideos();
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        List<VideoItem> result = new ArrayList<>();
        for (VideoItem item : videoApiClient.getFeaturedVideos()) {
            if (item.getTitle().toLowerCase(Locale.ROOT).contains(normalized)
                    || item.getSummary().toLowerCase(Locale.ROOT).contains(normalized)
                    || item.getAuthor().toLowerCase(Locale.ROOT).contains(normalized)) {
                result.add(item);
            }
        }
        return result;
    }
}
