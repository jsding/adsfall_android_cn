package com.overseas.gamesdk.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.nearme.game.sdk.GameCenterSDK;
import com.nearme.game.sdk.callback.ApiCallback;
import com.nearme.game.sdk.callback.GameExitCallback;
import com.nearme.game.sdk.common.config.Constants;
import com.nearme.game.sdk.common.model.biz.PayInfo;
import com.nearme.game.sdk.common.model.biz.ReqUserInfoParam;
import com.nearme.game.sdk.common.util.AppUtil;
import com.nearme.platform.opensdk.pay.PayResponse;
import com.overseas.gamesdk.demo.util.DemoUtil;
import com.overseas.gamesdk.demo.util.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private String mCurrencyCode = DemoUtil.getRegionCurrent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        //
        findViewById(R.id.test_login_activity).setOnClickListener(v -> sdkDoLoign());
        findViewById(R.id.test_pay).setOnClickListener(v -> doPay());
        findViewById(R.id.test_getuserinfo_by_cp_client).setOnClickListener(v -> doGetTokenAndSsoid());
        findViewById(R.id.test_get_region).setOnClickListener(v -> doGetRegion());
        findViewById(R.id.exit_btn).setOnClickListener(v -> sdkExit());
    }

    public void doGetRegion() {
        GameCenterSDK.getInstance().doGetRegion(new ApiCallback() {

            public void onSuccess(String resultMsg) {
                ToastUtil.show(MainActivity.this, resultMsg);
            }

            public void onFailure(String resultMsg, int resultCode) {
                ToastUtil.show(MainActivity.this, resultMsg + " " + resultCode);
            }
        });
    }

    private void sdkExit() {
        GameCenterSDK.getInstance().onExit(this, new GameExitCallback() {
            @Override
            public void exitGame() {
                AppUtil.exitGameProcess(MainActivity.this);
            }
        });
    }

    private void sdkDoLoign() {
        GameCenterSDK.getInstance().doLogin(this, new ApiCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                ToastUtil.show(MainActivity.this, resultMsg);
            }

            @Override
            public void onFailure(String resultMsg, int resultCode) {
                ToastUtil.show(MainActivity.this, resultMsg);
            }
        });
    }

    private PayInfo getPayInfo(int payType, int amount) {
        PayInfo payInfo = new PayInfo(System.currentTimeMillis() + new Random().nextInt(1000) + "", null, amount);
        String productDest = "demo description：";
        payInfo.setProductDesc(productDest);
        String productName = "GameCenterSDK Demo:";
        payInfo.setProductName(productName);
        payInfo.setType(payType);    //设置支付类型
        payInfo.setCountryCode(DemoUtil.getRegionCurrent());
        payInfo.setCurrency(mCurrencyCode);
        if (Constants.ENV == Constants.ENV_RELEASE) {
//            payInfo.setCallbackUrl("https://inal.open.cdo.oppo.local/sdklocal/callback/cp/validate"); //支付结果回调
        } else if (Constants.ENV == Constants.ENV_DEV) {
//            payInfo.setCallbackUrl("https://cdo-dev.wanyol.com:8001/sdklocal/callback/cp/validate");
        } else {
//            payInfo.setCallbackUrl("https://cn-game-test.wanyol.com/sdklocal/callback/cp/validate");
        }

        return payInfo;
    }

    private void doPay() {
        // CP 支付参数
        int amount = 1; // 支付金额，请以实际金额*100，最终的金额是 amount/100
        EditText payInput = findViewById(R.id.test_pay_input);
        String input = payInput.getText().toString();
        if (!TextUtils.isEmpty(input)) {
            try {
                amount = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                ToastUtil.show(MainActivity.this, "please input valid int value");
                return;
            }
        }

        PayInfo payInfo = getPayInfo(PayInfo.TYPE_NOARMAL_PAY, amount);

        GameCenterSDK.getInstance().doPay(this, payInfo, new ApiCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                ToastUtil.show(MainActivity.this, "支付成功");
            }

            @Override
            public void onFailure(String resultMsg, int resultCode) {
                if (PayResponse.CODE_CANCEL != resultCode) {
                    ToastUtil.show(MainActivity.this, "支付失败: " + resultMsg);
                } else {
                    // 取消支付处理
                    ToastUtil.show(MainActivity.this, "支付取消");
                }
            }
        });
    }

    public void doGetTokenAndSsoid() {
        GameCenterSDK.getInstance().doGetTokenAndSsoid(new ApiCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                try {
                    JSONObject json = new JSONObject(resultMsg);
                    String token = json.getString("token");
                    String ssoid = json.getString("ssoid");
                    doGetUserInfoByCpClient(token, ssoid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String content, int resultCode) {

            }
        });
    }

    private void doGetUserInfoByCpClient(String token, String ssoid) {
        GameCenterSDK.getInstance().doGetUserInfo(new ReqUserInfoParam(token, ssoid), new ApiCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                ToastUtil.show(MainActivity.this, resultMsg);
            }

            @Override
            public void onFailure(String resultMsg, int resultCode) {

            }
        });
    }
}