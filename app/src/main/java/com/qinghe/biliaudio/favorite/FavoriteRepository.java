package com.qinghe.biliaudio.favorite;

import com.qinghe.biliaudio.model.FavoriteCollection;

import java.util.Arrays;
import java.util.List;

public class FavoriteRepository {
    public List<FavoriteCollection> getCollections() {
        return Arrays.asList(
                new FavoriteCollection("默认收藏夹", 18, "用于快速收听的常用合集"),
                new FavoriteCollection("睡前故事", 7, "更适合短时收听的内容"),
                new FavoriteCollection("稍后再听", 12, "待补完的长音频与纪录片")
        );
    }
}
