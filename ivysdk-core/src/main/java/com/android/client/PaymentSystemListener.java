package com.android.client;


public interface PaymentSystemListener {

  void onPaymentSuccessWithPurchase(int bill, String orderId, String purchaseToken, String payload);

  void onPaymentFail(int bill);

  void onPaymentCanceled(int bill);

  void onPaymentSystemValid();

  void onPaymentSystemError(int causeId, String message);
}
