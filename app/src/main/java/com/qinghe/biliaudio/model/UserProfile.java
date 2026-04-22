package com.qinghe.biliaudio.model;

public class UserProfile {
    private final String name;
    private final int level;
    private final boolean loggedIn;
    private final String signature;

    public UserProfile(String name, int level, boolean loggedIn, String signature) {
        this.name = name;
        this.level = level;
        this.loggedIn = loggedIn;
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getSignature() {
        return signature;
    }
}
