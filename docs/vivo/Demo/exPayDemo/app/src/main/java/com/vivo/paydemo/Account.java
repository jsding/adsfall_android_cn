package com.vivo.paydemo;

/**
 * Created by XuPeng on 2018/5/22.
 */
public class Account {

    private static Account sAccount;
    private Holder mHolder;

    public static synchronized Account getInstance() {
        if (sAccount == null) {
            sAccount = new Account();
        }
        return sAccount;
    }

    private Account() {
    }

    public void onAccountLogin(String uid, String token) {
        mHolder = new Holder();
        mHolder.mUid = uid;
        mHolder.mToken = token;
    }

    public boolean isIsLogin() {
        return mHolder != null;
    }

    public String getUid() {
        if (mHolder == null) {
            return "";
        }
        return mHolder.mUid;
    }

    public String getToken() {
        return mHolder.mToken;
    }

    private static class Holder {
        private String mUid;
        private String mToken;
    }
}
