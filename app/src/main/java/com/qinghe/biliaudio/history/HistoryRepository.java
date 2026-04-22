package com.qinghe.biliaudio.history;

import com.qinghe.biliaudio.model.VideoItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryRepository {
    private static final int MAX_HISTORY_SIZE = 50;
    private final List<VideoItem> history = new ArrayList<>();

    public void recordPlay(VideoItem item) {
        if (item == null) {
            return;
        }
        history.removeIf(videoItem -> videoItem.getId().equals(item.getId()));
        history.add(0, item);
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
    }

    public List<VideoItem> getRecentHistory() {
        return new ArrayList<>(history);
    }
}
