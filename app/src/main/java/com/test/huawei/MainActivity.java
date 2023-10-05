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
    });

    AndroidSdk.onCreate(this, builder);

    dbAdapter = FirestoreAdapter.getInstance();


//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),
//                    PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//
//        AndroidSdk.Builder builder = new AndroidSdk.Builder();
//        builder.setSdkResultListener(new SdkResultListener() {
//            @Override
//            public void onInitialized() {
//                asyncToast("sdk initialized");
//            }
//
//            @Override
//            public void onReceiveServerExtra(String data) {
//                asyncToast("server data: " + data);
//            }
//
//            @Override
//            public void onReceiveNotificationData(String data) {
//                asyncToast("noti: " + data);
//            }
//        }).setUserCenterListener(new UserCenterListener() {
//            @Override
//            public void onReceiveLoginResult(boolean success) {
//                asyncToast("login? " + success);
//            }
//
//            @Override
//            public void onReceiveFriends(String friends) {
//                asyncToast("receive friends? " + friends);
//            }
//
//            @Override
//            public void onReceiveInviteResult(boolean success) {
//                asyncToast("invite? " + success);
//            }
//
//            @Override
//            public void onReceiveChallengeResult(int count) {
//                asyncToast("challenge? " + " count: " + count);
//            }
//
//            @Override
//            public void onReceiveLikeResult(boolean success) {
//                asyncToast("like? " + success);
//            }
//
//        }).setAdEventListener(new AdEventListener() {
//            @Override
//            public void onAdShow(String tag, String platform, int type){
//                super.onAdShow(tag, platform, type);
//                asyncToast("on Ad Show : " + tag);
//            }
//
//            @Override
//            public void onAdClicked(String tag, String platform, int type) {
//                super.onAdClicked(tag, platform, type);
//                asyncToast("on Ad Click : " + tag);
//            }
//        }).setPaymentListener(new PaymentSystemListener() {
//            @Override
//            public void onPaymentSuccess(int billId) {
//                asyncToast("payment success: " + billId);
//            }
//
//            @Override
//            public void onPaymentFail(int billId) {
//                asyncToast("payment fail: " + billId);
//            }
//
//            @Override
//            public void onPaymentCanceled(int bill) {
//                asyncToast("payment cancel: " + bill);
//            }
//
//            @Override
//            public void onPaymentSystemValid() {
//                Log.e("DEMO", "pay system is valid");
//            }
//        });
//
//        AndroidSdk.onCreate(this, builder);
//        AndroidSdk.loadExtra(1);

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
    switch (v.getId()) {
      case R.id.start:
        // IvySdk.showInterstitialAd();
        // true, null, SdkEnv.getUUID(), null, 0, useSound, soundName, userInfo
        // AndroidSdk.pushLocalMessage("hehehaha", "test title", "this is a push test", 30, 0, false, null, null);

        AndroidSdk.showFullAd("default");
        break;

      case R.id.pause:
        // IvySdk.showInterstitialAd();
        // IvySdk.showPopupInAppMessage("hehe", "您的ticket信息有更新", null, null);
        break;

      case R.id.exit:
        IvySdk.showInterstitialAd();

        break;

      case R.id.more:
//                IvySdk.moreGame();
        try {
          JSONObject data = new JSONObject();
          data.put("content", "???");
          AndroidSdk.helpshift("farmtownguest", data.toString());
//                    Unity.sendChat("https://mergeelves-chats.firebaseio.com/", "chats/jsding", data.toString());
        } catch (Throwable t) {
          t.printStackTrace();
        }
//                AndroidSdk.showWebView("来自精灵合成宝贝家的", "https://docs.google.com/forms/d/e/1FAIpQLSeVuRdssdP3isCvlWiLy4BoD9evJDzMKnYLlbc00cE5ot6S3g/viewform?usp=sf_link");
        // Unity.initializeAndLinkFacebookAfterSignIn();
        // throw new RuntimeException("Test Crash"); // Force a crash

//                AndroidSdk.loginFacebook(new FacebookLoginListener() {
//                    @Override
//                    public void onReceiveLoginResult(boolean success) {
//                        FirestoreAdapter.getInstance().initializeAndLinkFacebookAfterSignIn(new DatabaseConnectListener() {
//                            @Override
//                            public void onSuccess() {
//
//                            }
//
//                            @Override
//                            public void onFail() {
//
//                            }
//                        });
//                     }
//
//                    @Override
//                    public void onReceiveFriends(String friends) {
//
//                    }
//                });


        break;

      case R.id.banner:
//                bannerIdx = (++bannerIdx) % bannerPos.length;
        break;

      case R.id.bill:
        // Unity3dPlayerActivity.launch(this, 1);
//         IvySdk.pay("block.puzzle.removeads", "");

        AndroidSdk.pay(1, "dfafdsfdsfds", "dfadsfds");
//        IvySdk.executeCloudFunction("date", null, new OnCloudFunctionResult() {
//          @Override
//          public void onResult(String data) {
//
//          }
//
//          @Override
//          public void onFail(String errorMessage) {
//
//          }
//        });
        break;

      case R.id.close_banner:
        break;

      case R.id.custom:
//                AndroidSdk.verifyIdCard();
//                Logger.debug("AndroidSdk", AndroidSdk.hasNotch());
////                Logger.debug("AndroidSdk", AndroidSdk.getSKUDetail(1).toJson().toString());
////                Logger.debug("AndroidSdk", AndroidSdk.getSKUDetail(22).toJson().toString());
//
//                Logger.debug("AndroidSdk", AndroidSdk.getPurchaseHistory("subs"));
//                toast("notchHeight: " + AndroidSdk.getNotchHeight());
        // AndroidSdk.changeSku(22, 23, null);
        dbAdapter.read("users", new DatabaseListener() {
          @Override
          public void onData(String collection, String data) {
            System.out.println("------");
          }

          @Override
          public void onSuccess(String collection) {
            System.out.println("onSuccess");

          }

          @Override
          public void onFail(String collection) {
            System.out.println("onFail");

          }
        });
        break;

      case R.id.pass_level:
//        AndroidSdk.pay(22);
        IvySdk.showInAppMessage("这是一个测试的弹出消息，看看是傻燕子的都发到发疯 的说法似懂非懂！");
        break;

      case R.id.free:
//                if (AndroidSdk.hasRewardAd("default")) {
        AndroidSdk.showRewardAd("default", new AdListener() {

        });
//                } else {
//                    Toast.makeText(this, "no video ad", Toast.LENGTH_SHORT).show();
//                }
        break;

      case R.id.native_1:
        // AndroidSdk.showNativeBanner("default", 27, 94, 408, 290, null);
        // AndroidSdk.rateUs();
        toast("Notification status: " + IvySdk.isNotificationChannelEnabled(IvySdk.getActivity()));
        IvySdk.openNotificationSettings(IvySdk.getActivity());
//                if (nativeShowing) {
//                    AndroidSdk.hideNativeAdScrollView("unlock_pre");
//                } else {
//                    AndroidSdk.showNativeAdScrollView("unlock_pre", AndroidSdk.HIDE_BEHAVIOR_NO_HIDE, 50);
//                }
        nativeShowing = !nativeShowing;
        break;

      case R.id.share:
        AndroidSdk.shareOnFacebook("npc_1", new ShareResultListener() {
          @Override
          public void onSuccess(String postId) {

          }

          @Override
          public void onCancel() {

          }

          @Override
          public void onError(String message) {

          }
        });

        break;

      case R.id.islogin:
        // IvySdk.showDeliciousBanner(0, 0, 800, 1200, null);
//                toast("is login: " + AndroidSdk.isLogin());


        break;

      case R.id.login:
//                IvySdk.closeNativeAd();
//                AndroidSdk.login();

        Unity.signIn();
        break;

      case R.id.logout:
//                AndroidSdk.logout();
        Unity.signInWithEmailAndPassword("jsding2006@gmail.com", "kill666");
//        AndroidSdk.logoutGoogle(new GoogleListener() {
//          @Override
//          public void onSuccess(String googleId, String googleEmail) {
//            Log.d("AndroidSdk", "Google Logout");
//          }
//
//          @Override
//          public void onFails() {
//            Log.d("AndroidSdk", "Google Logout failed");
//
//          }
//        });
        break;

      case R.id.invite:
//                AndroidSdk.invite();
        break;

      case R.id.challenge:
        try {
        } catch (Exception ex) {

        }
//                AndroidSdk.challenge("haha title", "heihei message");
        break;

      case R.id.friends:
//                toast(AndroidSdk.friends());
        break;

      case R.id.me: {

//                try {
//                    String me1 = AndroidSdk.me();
//                    JSONObject me = new JSONObject(me1);
//                    if (me.has("picture")) {
//                        ImageView vv = new ImageView(this);
//                        vv.setImageURI(Uri.parse(me.getString("picture")));
//                        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(me.getString("name")).create();
//                        dialog.show();
//                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(256, 256);
//                        ((FrameLayout) dialog.getWindow().getDecorView()).addView(vv, lp);
//                    } else {
//                        toast(me1);
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
      }
      break;

      case R.id.like:
//                AndroidSdk.like();
//                AndroidSdk.appFeedback("https://merge-elves-38816766.web.app/english/sections.json", "https://us-central1-merge-elves-38816766.cloudfunctions.net/", "https://docs.google.com/forms/d/e/1FAIpQLSeOV9E2txOwLJNMnXA5GCbTSX_jVYsWGIQE9Bhvgq8ykIgR9w/viewform?usp=sf_link","{}");
        break;
//
//            case R.id.submit_score:
//                AndroidSdk.submitScore("endless", 232, "");
//                break;
//
//            case R.id.load_friend:
//                AndroidSdk.loadLeaderBoard("endless", 1, 20, "");
//                break;
//
      case R.id.load_global:
//                AndroidSdk.loadGlobalLeaderBoard("endless", 1, 20);

        IvySdk.logEvent("test_event", null);

        break;
//
//            case R.id.show_sales:
//                AndroidSdk.showSales(2);
//                break;


      case R.id.show_native_banner:
        break;
    }
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
