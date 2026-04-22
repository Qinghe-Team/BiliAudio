package com.qinghe.biliaudio.model;

public class CommentItem {
    private final String author;
    private final String content;

    public CommentItem(String author, String content) {
        this.author = author;
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }
}
