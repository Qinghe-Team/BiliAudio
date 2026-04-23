package com.qinghe.biliaudio.api;

import com.qinghe.biliaudio.model.UserProfile;

public class UserApiClient {
    public UserProfile getCurrentUser() {
        return new UserProfile("游客", 0, false, "未登录时可浏览推荐与搜索结果");
    }
}
