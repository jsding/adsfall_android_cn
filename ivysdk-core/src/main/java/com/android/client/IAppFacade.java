package com.android.client;

import android.content.Context;

public interface IAppFacade {
  void attachBaseContext(Context base);
  void onCreate(android.app.Application app);
}
