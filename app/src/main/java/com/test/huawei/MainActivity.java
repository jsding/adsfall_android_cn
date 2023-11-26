package com.test.huawei;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.adsfall.demo.R;
import com.android.client.AdListener;
import com.android.client.AndroidSdk;
import com.android.client.DatabaseListener;
import com.android.client.PaymentSystemListener;
import com.android.client.ShareResultListener;
import com.android.client.Unity;
import com.ivy.IvySdk;
import com.ivy.firestore.FirestoreAdapter;

import org.json.JSONObject;

/**
 * Created by song on 16/5/26.
 */
public class MainActivity extends Activity implements View.OnClickListener {
  FirestoreAdapter dbAdapter = null;

  private void hideSystemUI() {
    // Enables regular immersive mode.
    // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
    // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_IMMERSIVE
        // Set the content to appear under the system bars so that the
        // content doesn't resize when the system bars hide and show.
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        // Hide the nav bar and status bar
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN);
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      hideSystemUI();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    hideSystemUI();


    AndroidSdk.Builder builder = new AndroidSdk.Builder();

    builder.setPaymentListener(new PaymentSystemListener() {


      @Override
      public void onPaymentSuccessWithPurchase(int bill, String orderId, String purchaseToken, String payload) {

      }

      @Override
      public void onPaymentFail(int bill) {

      }

      @Override
      public void onPaymentCanceled(int bill) {

      }

      @Override
      public void onPaymentSystemValid() {

      }

      @Override
      public void onPaymentSystemError(int causeId, String message) {

      }

      @Override
      public void onSkuDetailData(int billId, String skuJson) {

      }
    });

    AndroidSdk.onCreate(this, builder);

    dbAdapter = FirestoreAdapter.getInstance();



    // ATTENTION: This was auto-generated to handle app links.
    Intent appLinkIntent = getIntent();
    String appLinkAction = appLinkIntent.getAction();
    Uri appLinkData = appLinkIntent.getData();
  }

  private static byte[] ivBytes = {0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30};

  void asyncToast(final String message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
      }
    });
  }

  boolean nativeShowing;
  boolean nativeBannerShowing;


  @Override
  public void onClick(View v) {
  }

  private void toast(final String message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onPause() {
    super.onPause();
    AndroidSdk.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    AndroidSdk.onResume();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    AndroidSdk.onNewIntent(intent);
  }

  @Override
  protected void onDestroy() {
    IvySdk.onDestroy();
    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    AndroidSdk.onActivityResult(requestCode, resultCode, data);
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onBackPressed() {
    IvySdk.onQuit();
  }
}
