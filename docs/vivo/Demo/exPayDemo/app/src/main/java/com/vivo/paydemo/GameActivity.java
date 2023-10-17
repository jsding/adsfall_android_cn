package com.vivo.paydemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.vivo.unionpay.sdk.open.VivoAccountCallback;
import com.vivo.unionpay.sdk.open.VivoConstants;
import com.vivo.unionpay.sdk.open.VivoPayCallback;
import com.vivo.unionpay.sdk.open.VivoPayInfo;
import com.vivo.unionpay.sdk.open.VivoRoleInfo;
import com.vivo.unionpay.sdk.open.VivoUnionSDK;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by XuPeng on 2018/5/22.
 */
public class GameActivity extends Activity implements View.OnClickListener {

    private Button mLoginButton;
    private EditText mProductName;
    private EditText mProductPrice;
    private EditText mServiceName;
    private EditText mServiceId;
    private EditText mRoleName;
    private EditText mRoleId;
    private EditText mRoleGrade;
    private RadioGroup mPayMethods;
    private Button mPayButton;
    private LogView mLogView;
    private Button mOrientView;
    private Button mClearLogView;
    private Button mVivoRoleNameBtn;
    private String mVivoServiceName;
    private String mVivoServiceId;
    private String mVivoRoleName;
    private String mVivoRoleId;
    private String mVivoRoleGrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_pay);
        initView();
        lockScreenOrientation();
        mLogView.flush();
    }

    private void initView() {
        mLoginButton = findViewById(R.id.vivo_pay_demo_login);
        mProductName = findViewById(R.id.vivo_pay_demo_product_name_input);
        mProductPrice = findViewById(R.id.vivo_pay_demo_product_price_input);
        mVivoRoleNameBtn = findViewById(R.id.vivo_pay_demo_role_name_input);
        mPayMethods = findViewById(R.id.vivo_pay_demo_method_group);
        mPayButton = findViewById(R.id.vivo_pay_demo_start_pay);
        mLogView = findViewById(R.id.vivo_pay_demo_log);
        mOrientView = findViewById(R.id.vivo_pay_demo_change_orient);
        mClearLogView = findViewById(R.id.vivo_pay_demo_clear_log);
        mLoginButton.setOnClickListener(this);
        mPayButton.setOnClickListener(this);
        mOrientView.setOnClickListener(this);
        mClearLogView.setOnClickListener(this);
        mVivoRoleNameBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.vivo_pay_demo_role_name_input:
            showDialog();
            break;
        case R.id.vivo_pay_demo_login:
            mLogView.printD(Constants.TIPS_LOGIN_START);
            onLoginClick();
            break;
        case R.id.vivo_pay_demo_clear_log:
            mLogView.clear();
            break;
        case R.id.vivo_pay_demo_start_pay:
            mLogView.printD(Constants.TIPS_PAY_START);
            onPayClick();
            break;
        case R.id.vivo_pay_demo_change_orient:
            mLogView.printW(getString(R.string.vivo_pay_demo_orientation));
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            break;
        }
    }

    private void showDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.item_role_edit, null);
        mServiceName = view.findViewById(R.id.vivo_pay_demo_service_name);
        mServiceId = view.findViewById(R.id.vivo_pay_demo_service_id);
        mRoleName = view.findViewById(R.id.vivo_pay_demo_role_name);
        mRoleId = view.findViewById(R.id.vivo_pay_demo_role_id);
        mRoleGrade = view.findViewById(R.id.vivo_pay_demo_role_grade);
        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mVivoServiceName = mServiceName.getText().toString();
                mVivoServiceId = mServiceId.getText().toString();
                mVivoRoleGrade = mRoleGrade.getText().toString();
                mVivoRoleId = mRoleId.getText().toString();
                mVivoRoleName = mRoleName.getText().toString();
                dialog.dismiss();
            }
        }).create().show();
    }

    /**
     * 如果没有接入vivo账户，则该方法可以忽略
     */
    private void onLoginClick() {
        VivoUnionSDK.login(GameActivity.this, new VivoAccountCallback() {
            @Override
            public void onVivoAccountLogin(int statusCode, String token, String uid) {
                if (statusCode == VivoConstants.VIVO_LOGIN_SUCCESS) {
                    String tips = Constants.TIPS_LOGIN_SUCCESS + "：\nuid=" + uid + "\ntoken=" + token;
                    Account.getInstance().onAccountLogin(uid, token);
                    mLogView.printD(tips);
                } else if (statusCode == VivoConstants.VIVO_LOGIN_FAILED) {
                    mLogView.printE(Constants.TIPS_LOGIN_FAILURE);
                } else {
                    mLogView.printW(Constants.TIPS_LOGIN_CANCEL);
                }
            }
        });
    }

    /**
     * 示例这里为了方便传的是ProductName和ProductPrice，建议使用productId，这样可以在收银台提供切换支付国家和货币功能
     */
    private void onPayClick() {
        String productName = mProductName.getText().toString();
        String productPrice = mProductPrice.getText().toString();
        VivoPayInfo payInfo = createPayInfo(productName, productPrice, Account.getInstance().getUid(),
                mVivoServiceName, mVivoServiceId, mVivoRoleName, mVivoRoleId, mVivoRoleGrade);

        VivoPayCallback callback = new VivoPayCallback() {
            @Override
            public void onVivoPayResult(int statusCode) {
                onPayResult(statusCode);
            }
        };
        if (mPayMethods.getCheckedRadioButtonId() == R.id.vivo_pay_demo_checkout) {
            VivoUnionSDK.pay(this, payInfo, callback);
        } else {
            VivoUnionSDK.pay(this, payInfo, callback, VivoConstants.VIVO_SMS_PAYMENT);
        }
    }

    private void onPayResult(int status) {
        switch (status) {
        case VivoConstants.VIVO_PAY_SUCCESS:
            mLogView.printD(Constants.TIPS_PAY_SUCCESS);
            break;
        case VivoConstants.VIVO_PAY_FAILED:
            mLogView.printE(Constants.TIPS_PAY_FAILURE);
            break;
        case VivoConstants.VIVO_PAY_INVALID_PARAM:
            mLogView.printE(Constants.TIPS_PAY_INVALID_PARAM);
            break;
        case VivoConstants.VIVO_PAY_ERROR:
            mLogView.printE(Constants.TIPS_PAY_ERROR);
            break;
        case VivoConstants.VIVO_PAY_OVER_TIME:
            mLogView.printE(Constants.TIPS_PAY_TIMEOUT);
            break;
        case VivoConstants.VIVO_PAY_CANCEL:
            mLogView.printW(Constants.TIPS_PAY_CANCEL);
            break;
        }
    }

    protected void lockScreenOrientation() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        }
    }

    public static VivoPayInfo createPayInfo(String productName, String productPrice, String uid, String serviceName,
                                            String serviceId, String roleName, String roleid, String rolegrade) {
        String cpOrder = UUID.randomUUID().toString().replaceAll("-", "");  //cp自定义传的订单号，保证订单号唯一即可
        Map<String, String> params = new HashMap<>();
        params.put(Constants.PARAM_APP_ID, Constants.GAME_APP_ID);
        params.put(Constants.PARAM_CP_ORDER_ID, cpOrder);
        params.put(Constants.PARAM_PRODUCT_NAME, productName);
        params.put(Constants.PARAM_PRODUCT_PRICE, productPrice);
        params.put(Constants.PARAM_EXT_INFO, Constants.APP_EXT_INFO);
        params.put(Constants.PARAM_PARTNER_OPEN_ID, uid);
        params.put(Constants.PARAM_NOTIFY_URL, Constants.APP_NOTIFY_URL);

        String sign = VivoSignUtils.getVivoSign(params, Constants.GAME_APP_SECRET);

        VivoRoleInfo roleInfo = new VivoRoleInfo();
        roleInfo.setServiceAreaName(serviceName);   //区服信息
        roleInfo.setServiceAreaId(serviceId);       //区服id
        roleInfo.setRoleName(roleName);             //角色名称
        roleInfo.setRoleId(roleid);                 //角色id
        roleInfo.setRoleGrade(rolegrade);           //角色等级

        VivoPayInfo vivoPayInfo = new VivoPayInfo.Builder().setExtInfo(Constants.APP_EXT_INFO)
                .setAppId(Constants.GAME_APP_ID).setNotifyUrl(Constants.APP_NOTIFY_URL).setProductName(productName).setProductPrice(productPrice).setSign(sign)
                .setSignType(Constants.PAY_SIGN_TYPE)
                .setUid(uid).setTransNo(cpOrder).setVivoRoleInfo(roleInfo)
                .build();
        return vivoPayInfo;


    }
}
