package com.ivy.huawei;

import android.app.Activity;
import android.content.Intent;

import com.android.client.IProviderFacade;
import com.android.client.OnPaymentSystemReadyListener;
import com.android.client.OnSignedInListener;
import com.android.client.UserCenterListener;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.AntiAddictionCallback;
import com.huawei.hms.jos.AppParams;
import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.JosStatusCodes;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.GamesStatusCodes;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.support.account.service.AccountAuthService;
import com.huawei.hms.utils.ResourceLoaderUtil;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;
import com.huawei.updatesdk.service.otaupdate.UpdateKey;
import com.ivy.IvySdk;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.impl.PurchaseManagerImpl;
import com.ivy.util.Logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.Serializable;

public class HuaweiProviderFacade implements IProviderFacade {
  private static final String TAG = "Huawei";

  @Override
  public void onInitialize(@NonNull Activity activity, @NonNull JSONObject gridData) {
  }


  @Override
  public void signIn(@NonNull Activity activity, @NonNull OnSignedInListener onSignedInListener) {
  }

  public boolean onlyUsingPlatformAccount() {
    return false;
  }

  @Override
  public PurchaseManager getPurchaseManager() {
    return new PurchaseManagerImpl();
  }

  @Override
  public void initPushSystem(@NonNull Activity activity) {

  }

  @Override
  public String getChannel() {
    return "huawei";
  }

  @Override
  public void onCreate(Activity activity) {
    if (activity == null) {
      Logger.error(TAG, "onCreate exception, activity null");
      return;
    }

    initHuaweiAccount(activity);
  }

  private boolean hasInitHuaweiAccount = false;
  private UserCenterListener userCenterListener = null;

  private OnPaymentSystemReadyListener onPaymentSystemReadyListener;

  private void initHuaweiAccount(@NonNull Activity activity) {
    AccountAuthParams params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME;
    JosAppsClient appsClient = JosApps.getJosAppsClient(activity);
    com.huawei.hmf.tasks.Task<Void> initTask;
    // 设置防沉迷提示语的Context，此行必须添加
    ResourceLoaderUtil.setmContext(activity);
    AppParams appParams = new AppParams(params, new AntiAddictionCallback() {
      @Override
      public void onExit() {
        // 该回调会在如下两种情况下返回:
        // 1.未成年人实名帐号在白天登录游戏，华为会弹框提示玩家不允许游戏，玩家点击“确定”，华为返回回调
        // 2.未成年实名帐号在国家允许的时间登录游戏，到晚上9点，华为会弹框提示玩家已到时间，玩家点击“知道了”，华为返回回调
        // 您可在此处实现游戏防沉迷功能，如保存游戏、调用帐号退出接口或直接游戏进程退出(如System.exit(0))
      }
    });

    initTask = appsClient.init(appParams);
    initTask.addOnSuccessListener(new OnSuccessListener<Void>() {
      @Override
      public void onSuccess(Void aVoid) {
        Logger.debug(TAG, "init success");
        hasInitHuaweiAccount = true;
        // 游戏初始化成功后需要调用一次浮标显示接口
        Games.getBuoyClient(activity).showFloatWindow();

        checkUpdate(activity);

        // 必须在init成功后，才可以实现登录功能
        signInHuaweiAccount(activity, null);
      }
    }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(Exception e) {
        if (e instanceof ApiException) {
          ApiException apiException = (ApiException) e;
          int statusCode = apiException.getStatusCode();
          if (statusCode == JosStatusCodes.JOS_PRIVACY_PROTOCOL_REJECTED) { // 错误码为7401时表示用户未同意华为联运隐私协议
            Logger.debug(TAG, "has reject the protocol");
            // 此处您需禁止玩家进入游戏
          } else if (statusCode == GamesStatusCodes.GAME_STATE_NETWORK_ERROR) { // 错误码7002表示网络异常
            Logger.debug(TAG, "Network error");
            // 此处您可提示玩家检查网络，请不要重复调用init接口，否则断网情况下可能会造成手机高耗电。
          } else if (statusCode == 907135003) {
            // 907135003表示玩家取消HMS Core升级或组件升级
            Logger.debug(TAG, "init statusCode=" + statusCode);
            initHuaweiAccount(activity);
          } else {
            // 在此处实现其他错误码的处理
          }
        }
      }
    });
  }

  private void signInHuaweiAccount(@NonNull Activity activity, @Nullable UserCenterListener listener) {
    userCenterListener = listener;
    AccountAuthParams authParams = new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM).setAuthorizationCode().createParams();
    AccountAuthService service = AccountAuthManager.getService(activity, authParams);
    com.huawei.hmf.tasks.Task<AuthAccount> task = service.silentSignIn();
    task.addOnSuccessListener(new OnSuccessListener<AuthAccount>() {
      @Override
      public void onSuccess(AuthAccount authAccount) {
        //获取帐号信息
        Logger.debug(TAG, "displayName:" + authAccount.getDisplayName());
        //获取帐号类型，0表示华为帐号、1表示AppTouch帐号
        Logger.debug(TAG, "accountFlag:" + authAccount.getAccountFlag());
        huaweiAccountSignedIn(authAccount);
        if (userCenterListener != null) {
          userCenterListener.onReceiveLoginResult(true);
        }
      }
    });

    task.addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(Exception e) {
        //登录失败，您可以尝试使用getSignInIntent()方法显式登录
        if (e instanceof ApiException) {
          ApiException apiException = (ApiException) e;
          Logger.debug(TAG, "sign failed status:" + apiException.getStatusCode());

          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              activity.startActivityForResult(service.getSignInIntent(), 8888);
            }
          });

        }
      }
    });
  }

  @Override
  public void onResume(Activity activity) {
    if (activity != null) {
      Games.getBuoyClient(activity).showFloatWindow();
    }
  }

  @Override
  public void onPause(Activity activity) {
    if (activity != null) {
      Games.getBuoyClient(activity).hideFloatWindow();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 8888) {
      com.huawei.hmf.tasks.Task<AuthAccount> authAccountTask = AccountAuthManager.parseAuthResultFromIntent(data);
      if (authAccountTask.isSuccessful()) {
        //登录成功，获取用户的帐号信息和Authorization Code
        AuthAccount authAccount = authAccountTask.getResult();
        Logger.debug(TAG, "serverAuthCode:" + authAccount.getAuthorizationCode());

        huaweiAccountSignedIn(authAccount);

        if (userCenterListener != null) {
          userCenterListener.onReceiveLoginResult(true);
        }
      } else {
        //登录失败
        Logger.error(TAG, "sign in failed:" + ((ApiException) authAccountTask.getException()).getStatusCode());
      }
    }
  }

  @Override
  public void registerPaymentSystemReadyListener(@NonNull OnPaymentSystemReadyListener listener) {
    onPaymentSystemReadyListener = listener;
  }

  private void checkUpdate(@NonNull Activity activity) {
    AppUpdateClient client = JosApps.getAppUpdateClient(activity);
    client.checkAppUpdate(activity, new CheckUpdateCallBack() {
      @Override
      public void onUpdateInfo(Intent intent) {
        if (intent != null) {
          // 更新状态信息
          int status = intent.getIntExtra(UpdateKey.STATUS, -99);
          Logger.debug(TAG, "check update status is:" + status);
          // 返回错误码
          int rtnCode = intent.getIntExtra(UpdateKey.FAIL_CODE, -99);
          // 返回失败信息
          String rtnMessage = intent.getStringExtra(UpdateKey.FAIL_REASON);
          // 强制更新应用时，弹出对话框后用户是否点击“退出应用”按钮
          boolean isExit = intent.getBooleanExtra(UpdateKey.MUST_UPDATE, false);
          Logger.debug(TAG, "rtnCode = " + rtnCode + "rtnMessage = " + rtnMessage);

          Serializable info = intent.getSerializableExtra(UpdateKey.INFO);
          // 如果info属于ApkUpgradeInfo类型，则拉起更新弹框
          if (info instanceof ApkUpgradeInfo) {
            // showUpdateDialog接口中最后一个字段传入不同取值会带来不同的用户体验，具体请参考本文档的场景描述，此处以false为例
            JosApps.getAppUpdateClient(activity).showUpdateDialog(activity, (ApkUpgradeInfo) info, false);
            Logger.debug(TAG, "check update success and there is a new update");
          }
          Logger.debug(TAG, "check update isExit=" + isExit);
          if (isExit) {
            // 是强制更新应用，用户在弹出的升级提示框中选择了“退出应用”，处理逻辑由您自行控制，这里只是个例子
            System.exit(0);
          }
        }
      }

      @Override
      public void onMarketInstallInfo(Intent intent) {

      }

      @Override
      public void onMarketStoreError(int i) {

      }

      @Override
      public void onUpdateStoreError(int i) {

      }
    });
  }

  private AuthAccount currentSignedInAuthAccount = null;

  private void huaweiAccountSignedIn(AuthAccount authAccount) {
    Activity activity = IvySdk.getActivity();
    onPaymentSystemReadyListener.onReady();

    currentSignedInAuthAccount = authAccount;
  }
}
