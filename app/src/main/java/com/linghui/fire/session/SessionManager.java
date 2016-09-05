package com.linghui.fire.session;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.utils.TaskInfoUtils;
import com.squareup.otto.Bus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

/**
 * Created by yhzhang on 2015/11/21.
 */
public class SessionManager {
    public static final String TAG = SessionManager.class.getSimpleName();

    public static final String ACCOUNT_TYPE = "com.linghui.fire";

    public static final int REAL_NAME_AUTH_STATUS_NOT_IDENTIFIED = 0;
    public static final int REAL_NAME_AUTH_STATUS_IN_REVIEW = 1;
    public static final int REAL_NAME_AUTH_STATUS_INDENTIFIED = 2;
    public static final int REAL_NAME_AUTH_STATUS_REJECTED = 3;

    private static final String SESSION_KEY_COOKIE = "cookie";
    private static final String SESSION_KEY_USER_ID = "user_id";
    private static final String SESSION_KEY_AVATAR_PATH = "avatar_path";
    private static final String SESSION_KEY_NICK_NAME = "nick_name";
    private static final String SESSION_KEY_MOBILE_NO = "mobileNo";
    private static final String SESSION_KEY_REAL_NAME_AUTH_STATUS = "real_name_auth_state";
    private static final String SESSION_KEY_HAS_SET_WITHDRAW_PASSWORD = "has_set_withdraw_password";
    private static final String SESSION_KEY_BOUND_TAOBAO_ACCOUNT = "bound_taobao_account";
    private static final String SESSION_KEY_AVAILABLE_POINTS = "available_points";
    private static final String SESSION_KEY_INTRODUCED_POINTS = "introduced_points";
    private static final String SESSION_KEY_EARNED_POINTS = "earned_points";
    private static final String SESSION_KEY_SPREADING_CODE = "spreading_code";
    private static final String SESSION_KEY_BANK_ACCOUNT_NAME = "bank_account_name";
    private static final String SESSION_KEY_BANK_ACCOUNT_NO = "bank_account_no";

    private SessionManager() {
        mBus = new Bus();
    }

    public static SessionManager getInstance() {
        if (sInstance == null) {
            synchronized (SessionManager.class) {
                if (sInstance == null) {
                    sInstance = new SessionManager();
                }
            }
        }

        return sInstance;
    }

    public synchronized void init(Context context) {
        mContext = context;
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        if (accounts != null && accounts.length > 0) {
            mAccount = accounts[0];
        }
    }

    public void create(String userId, String userName, String password) {
        List<HttpCookie> cookies = VolleyUtils.getInstance().getCookieManager().getCookieStore().get(URI.create(API.SERVER_ADDRESS));

        if (cookies != null && cookies.size() > 0) {
            AccountManager am = AccountManager.get(mContext);

            if (mAccount == null) {
                mAccount = new Account(userName, ACCOUNT_TYPE);
                am.addAccountExplicitly(mAccount, password, null);
            }
            am.setUserData(mAccount, SESSION_KEY_COOKIE, cookies.get(0).getValue());
            am.setUserData(mAccount, SESSION_KEY_USER_ID, userId);
        }
    }

    public boolean restore() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String cookieValue = am.getUserData(mAccount, SESSION_KEY_COOKIE);

            if (!TextUtils.isEmpty(cookieValue)) {
                HttpCookie cookie = new HttpCookie("JSESSIONID", cookieValue);
                cookie.setDomain(API.SERVER_DOMAIN);
                cookie.setPath("/");
                CookieStore cookieStore = VolleyUtils.getInstance().getCookieManager().getCookieStore();
                cookieStore.removeAll();
                cookieStore.add(URI.create(API.SERVER_ADDRESS), cookie);
                return true;
            }
        }

        return false;
    }

    public void remove() {
        VolleyUtils.getInstance().getCookieManager().getCookieStore().removeAll();
        if (mAccount != null) {
            Account lastAccount = mAccount;
            mAccount = null;

            AccountManager am = AccountManager.get(mContext);
            Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);

            if (accounts != null && accounts.length > 0) {
                if (Build.VERSION.SDK_INT >= 22) {
                    am.removeAccountExplicitly(lastAccount);
                } else {
                    am.removeAccount(lastAccount, null, null);
                }
            }
        }
    }

    public void syncUserInfo() {
        VolleyUtils.getInstance().sendRequest(API.getUserBasicInfoRequest(mGetUserBasicInfoResponseListener, mGetUserBasicInfoErrorListener));
    }

    public String getSessionCookie() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String cookieValue = am.getUserData(mAccount, SESSION_KEY_COOKIE);

            if (!TextUtils.isEmpty(cookieValue)) {
                return "JSESSIONID=" + cookieValue;
            }
        }

        return null;
    }

    public String getUserId() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            return am.getUserData(mAccount, SESSION_KEY_USER_ID);
        }

        return null;
    }

    public String getUserName() {
        if (mAccount != null) {
            return mAccount.name;
        }

        return null;
    }

    public String getAvatarPath() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String avatarPath = am.getUserData(mAccount, SESSION_KEY_AVATAR_PATH);
            return !TaskInfoUtils.isEmptyValue(avatarPath) ? avatarPath : null;
        }

        return null;
    }

    public String getNickName() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String nickName = am.getUserData(mAccount, SESSION_KEY_NICK_NAME);
            return !TaskInfoUtils.isEmptyValue(nickName) ? nickName : mContext.getString(R.string.default_nick_name);
        }

        return null;
    }

    public String getMobile() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            return am.getUserData(mAccount, SESSION_KEY_MOBILE_NO);
        }

        return null;
    }

    public int getRealNameAuthStatus() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String data = am.getUserData(mAccount, SESSION_KEY_REAL_NAME_AUTH_STATUS);

            if (!TextUtils.isEmpty(data)) {
                return Integer.valueOf(data);
            }
        }

        return REAL_NAME_AUTH_STATUS_NOT_IDENTIFIED;
    }

    public boolean getHasSetWithdrawPassword() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String data = am.getUserData(mAccount, SESSION_KEY_HAS_SET_WITHDRAW_PASSWORD);

            if (!TextUtils.isEmpty(data)) {
                return Boolean.valueOf(data);
            }
        }

        return false;
    }

    public String getTaobaoAccount() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            return am.getUserData(mAccount, SESSION_KEY_BOUND_TAOBAO_ACCOUNT);
        }

        return null;
    }

    public String getBankAccountName() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            return am.getUserData(mAccount, SESSION_KEY_BANK_ACCOUNT_NAME);
        }

        return null;
    }

    public String getBankAccountNo() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            return am.getUserData(mAccount, SESSION_KEY_BANK_ACCOUNT_NO);
        }

        return null;
    }

    public String getInviteCode() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            return am.getUserData(mAccount, SESSION_KEY_SPREADING_CODE);
        }

        return null;
    }

    public double getAvailablePoints() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String data = am.getUserData(mAccount, SESSION_KEY_AVAILABLE_POINTS);

            if (!TextUtils.isEmpty(data)) {
                return Double.valueOf(data);
            }
        }

        return 0;
    }

    public double getEarnedPoints() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String data = am.getUserData(mAccount, SESSION_KEY_EARNED_POINTS);

            if (!TextUtils.isEmpty(data)) {
                return Double.valueOf(data);
            }
        }

        return 0;
    }

    public double getIntroducedPoints() {
        if (mAccount != null) {
            AccountManager am = AccountManager.get(mContext);
            String data = am.getUserData(mAccount, SESSION_KEY_INTRODUCED_POINTS);

            if (!TextUtils.isEmpty(data)) {
                return Double.valueOf(data);
            }
        }

        return 0;
    }

    public Bus getEventBus() {
        return mBus;
    }

    private Response.Listener<JSONObject> mGetUserBasicInfoResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(final JSONObject response) {
            Log.d(TAG, "getUserBasicInfo : " + response);

            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... voids) {
                    if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                        JSONObject result = response.optJSONObject("result");
                        if (result != null) {
                            AccountManager am = AccountManager.get(mContext);
                            am.setUserData(mAccount, SESSION_KEY_AVATAR_PATH, result.optString("iconPath"));
                            am.setUserData(mAccount, SESSION_KEY_NICK_NAME, result.optString("nickName"));
                            am.setUserData(mAccount, SESSION_KEY_MOBILE_NO, result.optString("mobileNo"));
                            am.setUserData(mAccount, SESSION_KEY_AVAILABLE_POINTS, result.optString("availablePoints"));
                            am.setUserData(mAccount, SESSION_KEY_INTRODUCED_POINTS, result.optString("introducedPoints"));
                            am.setUserData(mAccount, SESSION_KEY_EARNED_POINTS, result.optString("earnedPoints"));
                            am.setUserData(mAccount, SESSION_KEY_SPREADING_CODE, result.optString("spreadingCode"));
                            am.setUserData(mAccount, SESSION_KEY_HAS_SET_WITHDRAW_PASSWORD, result.optString("withdrawPassword"));
                            am.setUserData(mAccount, SESSION_KEY_REAL_NAME_AUTH_STATUS, result.optString("identiStatus"));

                            JSONArray thirdParties = result.optJSONArray("thirdParties");
                            if (thirdParties != null && thirdParties.length() > 0) {
                                for (int i = 0; i < thirdParties.length(); i++) {
                                    JSONObject thirdParty = thirdParties.optJSONObject(i);
                                    String thirdPartyName = thirdParty.optString("partyName");
                                    int thirdPartyType = thirdParty.optInt("thirdPartyType");

                                    switch (thirdPartyType) {
                                        case 1: // taobao
                                            am.setUserData(mAccount, SESSION_KEY_BOUND_TAOBAO_ACCOUNT, thirdPartyName);
                                            break;
                                    }
                                }
                            }

                            JSONObject bankAccount = result.optJSONObject("bankAccount");
                            if (bankAccount != null) {
                                am.setUserData(mAccount, SESSION_KEY_BANK_ACCOUNT_NAME, bankAccount.optString("accountName"));
                                am.setUserData(mAccount, SESSION_KEY_BANK_ACCOUNT_NO, bankAccount.optString("accountNo"));
                            }

                            return true;
                        }
                    }

                    return false;
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    mBus.post(new SessionChangedEvent());
                }
            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    };

    private Response.ErrorListener mGetUserBasicInfoErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "getUserBasicInfo error : " + error);
        }
    };

    private static volatile SessionManager sInstance;

    private Context mContext;
    private Account mAccount;
    private Bus mBus;
}
