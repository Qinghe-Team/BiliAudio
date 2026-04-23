package com.qinghe.biliaudio.history;

import com.qinghe.biliaudio.model.VideoItem;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class HistoryRepositoryTest {
    @Test
    public void latestPlayedItemShouldBeFirstAndUnique() {
        HistoryRepository repository = new HistoryRepository();
        VideoItem first = new VideoItem("1", "A", "authorA", "summaryA", "10分钟", "urlA");
        VideoItem second = new VideoItem("2", "B", "authorB", "summaryB", "20分钟", "urlB");

        repository.recordPlay(first);
        repository.recordPlay(second);
        repository.recordPlay(first);

        List<VideoItem> history = repository.getRecentHistory();
        Assert.assertEquals(2, history.size());
        Assert.assertEquals("1", history.get(0).getId());
        Assert.assertEquals("2", history.get(1).getId());
    }
}
