package com.qinghe.biliaudio.comment;

import com.qinghe.biliaudio.model.CommentItem;

import java.util.Arrays;
import java.util.List;

public class CommentRepository {
    public List<CommentItem> getCommentsForVideo(String videoId) {
        return Arrays.asList(
                new CommentItem("听友A", "适合通勤或者手表外放场景。"),
                new CommentItem("听友B", "希望后续增加断点续播和历史同步。"),
                new CommentItem("听友C", "这个界面布局在方屏设备上比较顺手。")
        );
    }
}
