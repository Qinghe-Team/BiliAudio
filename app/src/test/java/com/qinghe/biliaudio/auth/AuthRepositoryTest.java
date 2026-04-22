package com.qinghe.biliaudio.auth;

import com.qinghe.biliaudio.api.AuthApiClient;
import com.qinghe.biliaudio.model.AuthMethod;
import com.qinghe.biliaudio.model.UserProfile;

import org.junit.Assert;
import org.junit.Test;

public class AuthRepositoryTest {
    @Test
    public void loginAndLogoutShouldUpdateCurrentUser() {
        AuthRepository repository = new AuthRepository(new AuthApiClient());

        UserProfile loginUser = repository.login(AuthMethod.QR_CODE);
        Assert.assertTrue(loginUser.isLoggedIn());
        Assert.assertTrue(repository.getCurrentUser().isLoggedIn());

        UserProfile logoutUser = repository.logout();
        Assert.assertFalse(logoutUser.isLoggedIn());
        Assert.assertFalse(repository.getCurrentUser().isLoggedIn());
    }
}
