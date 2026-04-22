package com.qinghe.biliaudio;

import android.app.Application;

import com.qinghe.biliaudio.core.AppContainer;

public class BiliAudioApplication extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
