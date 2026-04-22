package com.qinghe.biliaudio.search;

import com.qinghe.biliaudio.api.SearchApiClient;
import com.qinghe.biliaudio.model.VideoItem;

import java.util.List;

public class SearchRepository {
    private final SearchApiClient searchApiClient;

    public SearchRepository(SearchApiClient searchApiClient) {
        this.searchApiClient = searchApiClient;
    }

    public List<String> getHotKeywords() {
        return searchApiClient.getHotKeywords();
    }

    public List<VideoItem> search(String query) {
        return searchApiClient.search(query);
    }
}
