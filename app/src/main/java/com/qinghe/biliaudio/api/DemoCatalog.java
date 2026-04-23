package com.qinghe.biliaudio.api;

import com.qinghe.biliaudio.model.VideoItem;

import java.util.ArrayList;
import java.util.List;

final class DemoCatalog {
    private DemoCatalog() {
    }

    static List<VideoItem> videos() {
        List<VideoItem> items = new ArrayList<>();
        items.add(new VideoItem("bv1001", "《三体》广播剧·第一集", "科幻放映厅", "适合碎片时间收听的长篇科幻内容。", "42分钟", "https://example.com/audio/santi-1.mp3"));
        items.add(new VideoItem("bv1002", "古风故事会：长安夜雨", "听书阁", "偏剧情向的古风有声短篇。", "18分钟", "https://example.com/audio/changan.mp3"));
        items.add(new VideoItem("bv1003", "名著速读：西游记·火焰山", "青禾读书", "适合儿童手表场景的简短章节。", "11分钟", "https://example.com/audio/journey.mp3"));
        items.add(new VideoItem("bv1004", "B 站热门纪录片解说", "纪录片精选", "资讯和知识类内容的音频化入口示例。", "27分钟", "https://example.com/audio/doc.mp3"));
        return items;
    }
}
