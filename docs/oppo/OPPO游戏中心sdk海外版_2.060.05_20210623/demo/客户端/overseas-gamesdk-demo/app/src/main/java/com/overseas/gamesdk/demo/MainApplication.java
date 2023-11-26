package com.overseas.gamesdk.demo;

import android.app.Application;
import android.util.Log;

import com.nearme.game.sdk.GameCenterSDK;

public class MainApplication extends Application {
    private static final String TAG = "MainApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, getClass().getSimpleName() + ": onCreate = ");
        if (ProcessUtil.isMainProcess(this)) {
            onMainCreate();
        }
    }

    //主进程
    private final void onMainCreate() {
        Log.d(TAG, getClass().getSimpleName() + ": onMainCreate = ");
        //init sdk：sdk跑在独立进程，这里只需要在主进程做一次初始化操作。
        String appSecret = "16141caa0bed46a6afe6a6e09818d577";
        GameCenterSDK.init(appSecret, this);
    }
}
