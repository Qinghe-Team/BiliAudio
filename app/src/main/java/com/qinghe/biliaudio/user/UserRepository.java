package com.qinghe.biliaudio.user;

import com.qinghe.biliaudio.api.UserApiClient;
import com.qinghe.biliaudio.model.UserProfile;

public class UserRepository {
    private final UserApiClient userApiClient;

    public UserRepository(UserApiClient userApiClient) {
        this.userApiClient = userApiClient;
    }

    public UserProfile getCurrentUser() {
        return userApiClient.getCurrentUser();
    }
}
