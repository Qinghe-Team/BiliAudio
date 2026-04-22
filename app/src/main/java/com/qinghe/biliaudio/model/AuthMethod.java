package com.qinghe.biliaudio.model;

public enum AuthMethod {
    QR_CODE("扫码登录"),
    SMS_CODE("验证码登录"),
    PASSWORD("密码登录");

    private final String displayName;

    AuthMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
