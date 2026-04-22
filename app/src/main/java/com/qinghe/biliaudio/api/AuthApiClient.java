package com.qinghe.biliaudio.api;

import com.qinghe.biliaudio.model.AuthMethod;
import com.qinghe.biliaudio.model.UserProfile;

import java.util.Arrays;
import java.util.List;

public class AuthApiClient {
    public List<AuthMethod> getSupportedMethods() {
        return Arrays.asList(AuthMethod.values());
    }

    public UserProfile login(AuthMethod method) {
        return new UserProfile("青禾用户", 6, true, "已通过" + method.getDisplayName() + "进入手表听书模式");
    }
}
