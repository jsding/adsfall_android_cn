package com.vivo.paydemo;

import android.app.Application;
import android.util.Log;

import com.vivo.unionpay.sdk.open.VivoUnionSDK;

public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            VivoUnionSDK.initSdk(this, Constants.GAME_APP_ID);
        } catch (Exception e) {
            Log.e("DemoApplication", e.getMessage());
        }
    }
}
