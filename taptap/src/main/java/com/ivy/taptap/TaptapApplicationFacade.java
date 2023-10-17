package com.ivy.taptap;

import android.content.Context;

import androidx.annotation.Keep;

import com.android.client.IAppFacade;
import com.taptap.payment.shell.TapPayment;

@Keep
public class TaptapApplicationFacade implements IAppFacade {
  @Override
  public void attachBaseContext(Context base) {
    TapPayment.init(base);
  }

  @Override
  public void onCreate(Context base) {

  }
}
