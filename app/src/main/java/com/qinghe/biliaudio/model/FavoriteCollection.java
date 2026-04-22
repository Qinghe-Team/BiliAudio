package com.qinghe.biliaudio.model;

public class FavoriteCollection {
    private final String name;
    private final int itemCount;
    private final String description;

    public FavoriteCollection(String name, int itemCount, String description) {
        this.name = name;
        this.itemCount = itemCount;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getItemCount() {
        return itemCount;
    }

    public String getDescription() {
        return description;
    }
}
