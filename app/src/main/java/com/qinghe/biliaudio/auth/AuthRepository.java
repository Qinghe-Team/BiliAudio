package com.qinghe.biliaudio.auth;

import com.qinghe.biliaudio.api.AuthApiClient;
import com.qinghe.biliaudio.model.AuthMethod;
import com.qinghe.biliaudio.model.UserProfile;

import java.util.List;

public class AuthRepository {
    private final AuthApiClient authApiClient;
    private UserProfile currentUser = new UserProfile("游客", 0, false, "未登录时可浏览推荐与搜索结果");

    public AuthRepository(AuthApiClient authApiClient) {
        this.authApiClient = authApiClient;
    }

    public List<AuthMethod> getSupportedMethods() {
        return authApiClient.getSupportedMethods();
    }

    public UserProfile login(AuthMethod method) {
        currentUser = authApiClient.login(method);
        return currentUser;
    }

    public UserProfile logout() {
        currentUser = new UserProfile("游客", 0, false, "已退出登录");
        return currentUser;
    }

    public UserProfile getCurrentUser() {
        return currentUser;
    }
}
