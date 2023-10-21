package com.ivy.adm;

import com.amazon.device.messaging.ADMMessageReceiver;

public class LisADMMessageReceiver extends ADMMessageReceiver {
  private static final int JOB_ID = 9;

  public LisADMMessageReceiver() {
    boolean ADMLatestAvailable = false;
    try {
      Class.forName("com.amazon.device.messaging.ADMMessageHandlerJobBase");
      ADMLatestAvailable = true;
    } catch (ClassNotFoundException e) {
      // 处理异常。
    }

    try {
      // 若可能，推荐使用基于新作业的
      if (ADMLatestAvailable) {
        registerJobServiceClass(LisADMMessageHandler.class, JOB_ID);
      }
    } catch(Throwable ignored) {

    }
  }
}