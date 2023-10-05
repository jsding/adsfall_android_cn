package com.ivy.fcm;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ivy.IvySdk;
import com.ivy.util.Logger;

import java.util.Map;


public class AOEFirebaseMessagingService extends FirebaseMessagingService {

  private static final String TAG = "MyFirebaseMsgService";

  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    Log.w(TAG, "onMessageReceived >>>>> ");
    super.onMessageReceived(remoteMessage);
    try {
      // TODO(developer): Handle FCM messages here.
      // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
      Log.d(TAG, "From: " + remoteMessage.getFrom());

      // Check if message contains a data payload.
      Map<String, String> data = remoteMessage.getData();
      if (data.size() > 0) {
        Logger.debug(TAG, "Message data payload: " + remoteMessage.getData());
      } else if (remoteMessage.getNotification() != null) {
        Logger.debug(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
      }
    } catch (Throwable t) {
      Logger.debug(TAG, "Push data exception", t);
    }
  }

  @Override
  public void onNewToken(@NonNull String token) {
    Logger.debug(TAG, "Refreshed token: " + token);
    sendRegistrationToServer(token);
  }

  private void sendRegistrationToServer(String token) {
  }
}