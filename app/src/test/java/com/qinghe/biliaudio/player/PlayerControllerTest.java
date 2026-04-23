package com.qinghe.biliaudio.player;

import com.qinghe.biliaudio.api.PlayerApiClient;
import com.qinghe.biliaudio.api.VideoApiClient;
import com.qinghe.biliaudio.model.PlaybackSettings;

import org.junit.Assert;
import org.junit.Test;

public class PlayerControllerTest {
    @Test
    public void customSpeedIsClampedToSupportedRange() {
        PlayerController controller = new PlayerController(null, new PlayerApiClient(), new VideoApiClient());

        PlaybackSettings settings = controller.setPlaybackSpeed(5.0);

        Assert.assertEquals(3.0, settings.getPlaybackSpeed(), 0.0);
    }
}
