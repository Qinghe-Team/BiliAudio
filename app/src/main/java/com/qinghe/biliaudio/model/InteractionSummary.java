package com.qinghe.biliaudio.model;

public class InteractionSummary {
    private final int likes;
    private final int coins;
    private final int favorites;
    private final int comments;

    public InteractionSummary(int likes, int coins, int favorites, int comments) {
        this.likes = likes;
        this.coins = coins;
        this.favorites = favorites;
        this.comments = comments;
    }

    public int getLikes() {
        return likes;
    }

    public int getCoins() {
        return coins;
    }

    public int getFavorites() {
        return favorites;
    }

    public int getComments() {
        return comments;
    }
}
