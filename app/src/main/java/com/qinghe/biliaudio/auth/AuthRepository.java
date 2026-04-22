package com.qinghe.biliaudio.auth;

import com.qinghe.biliaudio.api.AuthApiClient;
import com.qinghe.biliaudio.model.AuthMethod;
import com.qinghe.biliaudio.model.UserProfile;

import java.util.List;

public class AuthRepository {
    private final AuthApiClient authApiClient;

    public AuthRepository(AuthApiClient authApiClient) {
        this.authApiClient = authApiClient;
    }

    public List<AuthMethod> getSupportedMethods() {
        return authApiClient.getSupportedMethods();
    }

    public UserProfile login(AuthMethod method) {
        return authApiClient.login(method);
    }
}
