package com.linghui.fire.server;

import android.text.TextUtils;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.linghui.fire.BuildConfig;
import com.linghui.fire.session.SessionManager;
import junit.framework.Assert;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;


/**
 * Created by Yanghai on 2015/10/1.
 */
public class API {

    public static final int RESPONSE_STATUS_OK = 200;
    public static final int RESPONSE_STATUS_CREATED = 201;
    public static final int RESPONSE_STATUS_DELETED = 204;

    public static final String SERVER_SCHEME = "http://";
    public static final String SERVER_DOMAIN = BuildConfig.DEBUG ? "127.0.0.1" : "127.0.0.1";
    public static final String SERVER_ADDRESS = SERVER_SCHEME + SERVER_DOMAIN;

    // 七牛图片服务器地址
    public static final String getPictureUrlFromQiNiu(String key) {
        return "http://7xlgqa.com1.z0.glb.clouddn.com/" + key;
    }

    // 获取用户注册短信验证码
    private static final String GET_SMS_CODE_URL = SERVER_ADDRESS + "/register/mobile?mobileNo=";
    public static JsonObjectRequest getSMSCodeRequest(String mobileNo, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(mobileNo));

        return new CustomRequest(Request.Method.POST, GET_SMS_CODE_URL + mobileNo, listener, errorListener);
    }

    // 用户注册
    private static final String USER_REGISTER_URL = SERVER_ADDRESS + "/register/mobile/confirmation?";
    public static JsonObjectRequest userRegisterRequest(String mobileNo, String verificationCode, String password, String macAddress, String inviteCode,
                                                        Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(mobileNo));
        Assert.assertTrue(!TextUtils.isEmpty(verificationCode));
        Assert.assertTrue(!TextUtils.isEmpty(password));
        Assert.assertTrue(!TextUtils.isEmpty(macAddress));

        StringBuilder sb = new StringBuilder(USER_REGISTER_URL);
        sb.append("mobileNo=").append(mobileNo)
                .append("&smsCheckCode=").append(verificationCode)
                .append("&password=").append(password)
                .append("&mac=").append(macAddress);
        if (!TextUtils.isEmpty(inviteCode)) {
            sb.append("&spreadingCode=").append(inviteCode);
        }

        return new CustomRequest(Request.Method.POST, sb.toString(), "", listener, errorListener);
    }

    // 用户登录
    private static final String USER_LOGIN_URL = SERVER_ADDRESS + "/login?";
    public static JsonObjectRequest userLoginRequest(String mobileNo, String password, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(mobileNo));
        Assert.assertTrue(!TextUtils.isEmpty(password));

        StringBuilder sb = new StringBuilder(USER_LOGIN_URL);
        sb.append("userAccount=").append(mobileNo).append("&password=").append(password);

        return new CustomRequest(Request.Method.POST, sb.toString(), "", listener, errorListener);
    }

    // 重置登录密码短信验证码
    private static final String RESET_LOGIN_PASSWORD_SMS_CODE_URL = SERVER_ADDRESS + "/pwdreset/identiCode?mobileNo=";
    public static JsonObjectRequest resetLoginPasswordSMSCodeRequest(String mobileNo, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(mobileNo));

        return new CustomRequest(Request.Method.POST, RESET_LOGIN_PASSWORD_SMS_CODE_URL + mobileNo, listener, errorListener);
    }

    // 重置登陆密码
    private static final String RESET_LOGIN_PASSWORD_URL = SERVER_ADDRESS + "/pwdreset/bysms/password?";
    public static JsonObjectRequest resetLoginPasswordRequest(String mobileNo, String smsCheckCode, String password, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(mobileNo));
        Assert.assertTrue(!TextUtils.isEmpty(smsCheckCode));
        Assert.assertTrue(!TextUtils.isEmpty(password));

        StringBuilder sb = new StringBuilder(RESET_LOGIN_PASSWORD_URL);
        sb.append("mobileNo=").append(mobileNo)
                .append("&smsCheckCode=").append(smsCheckCode)
                .append("&password=").append(password)
                .append("&_method=put");
        return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
    }

    // 重置支付密码短信验证码
    private static final String RESET_WITHDRAW_PASSWORD_SMS_CODE_URL = SERVER_ADDRESS + "/users/%s/withdrawPassword/sms?";
    public static JsonObjectRequest resetWithdrawPasswordSMSCodeRequest(String mobileNo, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(mobileNo));

        String url = String.format(RESET_WITHDRAW_PASSWORD_SMS_CODE_URL, SessionManager.getInstance().getUserId());
        url += "mobileNo=" + mobileNo;
        return new CustomRequest(Request.Method.POST, url, listener, errorListener);
    }

    // 重置支付密码
    private static final String RESET_WITHDRAW_PASSWORD_URL = SERVER_ADDRESS + "/users/%s/withdrawPassword?";
    public static JsonObjectRequest resetWithdrawPasswordRequest(String smsCheckCode, String password, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(smsCheckCode));
        Assert.assertTrue(!TextUtils.isEmpty(password));

        StringBuilder sb = new StringBuilder(String.format(RESET_WITHDRAW_PASSWORD_URL, SessionManager.getInstance().getUserId()));
        sb.append("smsCheckCode=").append(smsCheckCode).append("&password=").append(password);
        return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
    }

    // 绑定第三方账号
    private static final String BINDING_THIRDPARTIES_URL = SERVER_ADDRESS + "/users/%s/thirdParties?";
    public static JsonObjectRequest bindingThirdpartiesRequest(String accountName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(accountName));

        try {
            StringBuilder sb = new StringBuilder(String.format(BINDING_THIRDPARTIES_URL, SessionManager.getInstance().getUserId()));
            sb.append("thirdPartyType=1").append("&isPublisher=N").append("&partyName=").append(URLEncoder.encode(accountName, "utf-8"));

            return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 绑定财付通
    private static final String BINDING_BANK_ACCOUNTS_URL = SERVER_ADDRESS + "/users/%s/bankAccounts?";
    public static final int BANK_ACCOUNT_ID_TENPAY = 9;
    public static JsonObjectRequest bindingBankAccountsRequest(int bankId, String accountName, String accountNo, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(accountName));
        Assert.assertTrue(!TextUtils.isEmpty(accountNo));

        StringBuilder sb = new StringBuilder(String.format(BINDING_BANK_ACCOUNTS_URL, SessionManager.getInstance().getUserId()));
        sb.append("bankId=").append(bankId)
                .append("&accountName=").append(accountName)
                .append("&accountNo=").append(accountNo);
        return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
    }

    // 获得实名认证上传信息
    private static final String REAL_NAME_AUTH_INFO_URL = SERVER_ADDRESS + "/users/%s/identity/attaches";
    public static JsonObjectRequest realNameAuthInfoRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new CustomRequest(Request.Method.GET, String.format(REAL_NAME_AUTH_INFO_URL, SessionManager.getInstance().getUserId()), listener, errorListener);
    }

    // 提交实名认证请求
    private static final String REAL_NAME_AUTH_URL = SERVER_ADDRESS + "/users/%s/identity/random?";
    public static JsonObjectRequest realNameAuthRequest(String identityNo, String attaches, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(identityNo));
        Assert.assertTrue(!TextUtils.isEmpty(attaches));

        StringBuilder sb = new StringBuilder(String.format(REAL_NAME_AUTH_URL, SessionManager.getInstance().getUserId()));
        try {
            sb.append("identityNo=").append(identityNo)
                    .append("&attaches=").append(URLEncoder.encode(attaches, "utf-8"));
            return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 获得上传图片的token
    private static final String GET_STORAGE_UPLOAD_TOKEN_URL = SERVER_ADDRESS + "/token/storage/upload";
    public static JsonObjectRequest getStorageUploadTokenRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new CustomRequest(Request.Method.GET, GET_STORAGE_UPLOAD_TOKEN_URL, listener, errorListener);
    }

    // 大厅任务列表
    private static final String HALL_TASK_LIST_URL = SERVER_ADDRESS + "/tasks?";
    public static final int DEFAULT_TASK_LIST_PAGE_SIZE = 15;
    public static JsonObjectRequest hallTaskListRequest(int pageSize, String timeStamp, Boolean old, Integer taskCategory, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        StringBuilder sb = new StringBuilder(HALL_TASK_LIST_URL);

        if (pageSize <= 0) {
            sb.append("pageSize=").append(DEFAULT_TASK_LIST_PAGE_SIZE);
        } else {
            sb.append("pageSize=").append(pageSize);
        }

        if (timeStamp != null) {
            try {
                String encodedTimeStamp = URLEncoder.encode(timeStamp, "utf-8");
                sb.append("&timeStamp=").append(encodedTimeStamp);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        if (old != null) {
            sb.append("&old=").append(old.booleanValue() ? 1 : 0);
        }

        if (taskCategory != null) {
            sb.append("&taskCategory=").append(taskCategory.intValue());
        }

        return new CustomRequest(Request.Method.GET, sb.toString(), listener, errorListener);
    }

    // 大厅广告
    private static final String HALL_ADVERTISEMENTS_URL = SERVER_ADDRESS + "/advertisements?adverType=1";
    public static JsonObjectRequest hallAdvertisementsRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new CustomRequest(Request.Method.GET, HALL_ADVERTISEMENTS_URL, listener, errorListener);
    }

    // 任务抢单
    private static final String ASSIGN_TASK_URL = SERVER_ADDRESS + "/users/%s/assignments?";
    public static final int TASK_ASSIGNMENT_ERROR_CODE_NO_REAL_NAME_AUTH = 2000001;
    public static final int TASK_ASSIGNMENT_ERROR_CODE_DOING_REAL_NAME_AUTH = 2000002;
    public static final int TASK_ASSIGNMENT_ERROR_CODE_NOT_BIND_TENPAY = 2000004;
    public static final int TASK_ASSIGNMENT_ERROR_CODE_NOT_BIND_TAOBAO = 2000007;
    public static final int TASK_ASSIGNMENT_ERROR_CODE_TAOBAO_HAS_BOUND = 2000008;
    public static final int TASK_ASSIGNMENT_ERROR_CODE_NO_TASK_LEFT = 2000011;
    public static final int TASK_ASSIGNMENT_ERROR_CODE_NOT_PASS_TEST = 2000016;
    public static JsonObjectRequest assignTaskRequest(String taskId, String reqTags, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId));
        Assert.assertTrue(reqTags != null);

        StringBuilder sb = new StringBuilder(String.format(ASSIGN_TASK_URL, SessionManager.getInstance().getUserId()));
        sb.append("taskId=").append(taskId).append("&reqTags=").append(reqTags);
        return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
    }

    // 获取用户信息
    private static final String GET_USER_BASIC_INFO_URL = SERVER_ADDRESS + "/users/%s/basicInfo";
    public static JsonObjectRequest getUserBasicInfoRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new CustomRequest(Request.Method.GET, String.format(GET_USER_BASIC_INFO_URL, SessionManager.getInstance().getUserId()), listener, errorListener);
    }

    // 提交用户信息
    private static final String SUBMIT_USER_INFO_URL = GET_USER_BASIC_INFO_URL + "?";
    public static JsonObjectRequest submitUserInfoRequest(Map<String, String> params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(params != null);

        StringBuilder sb = new StringBuilder(String.format(SUBMIT_USER_INFO_URL, SessionManager.getInstance().getUserId()));
        sb.append("_method=PUT");

        Set<Map.Entry<String, String>> entries = params.entrySet();
        for (Map.Entry entry : entries) {
            try {
                String encodedString = URLEncoder.encode((String)entry.getValue(), "utf-8");
                sb.append("&").append(entry.getKey()).append("=").append(encodedString);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
    }

    // 已抢任务列表
    private static final String ASSIGNED_TASK_list_URL = SERVER_ADDRESS + "/users/%s/assignments?";
    public static JsonObjectRequest assignedTaskListRequest(int pageSize, int finishStatus, String timeStamp, Boolean old, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        StringBuilder sb = new StringBuilder(String.format(ASSIGNED_TASK_list_URL, SessionManager.getInstance().getUserId()));

        if (pageSize <= 0) {
            sb.append("pageSize=").append(DEFAULT_TASK_LIST_PAGE_SIZE);
        } else {
            sb.append("pageSize=").append(pageSize);
        }
        sb.append("&finishStatus=").append(finishStatus <= 0 ? 0 : 1);

        if (timeStamp != null) {
            try {
                String encodedTimeStamp = URLEncoder.encode(timeStamp, "utf-8");
                sb.append("&timeStamp=").append(encodedTimeStamp);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        if (old != null) {
            sb.append("&old=").append(old.booleanValue() ? 1 : 0);
        }

        return new CustomRequest(Request.Method.GET, sb.toString(), listener, errorListener);
    }

    // 删除任务
    private static final String DELETE_ASSIGNED_TASK_URL = SERVER_ADDRESS + "/users/%s/assignments/%s?";
    public static JsonObjectRequest deleteAssignedTaskRequest(String assignId, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(assignId));

        StringBuilder sb = new StringBuilder(String.format(DELETE_ASSIGNED_TASK_URL,
                SessionManager.getInstance().getUserId(), assignId));
        sb.append("_method=delete");

        return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
    }

    // 确认返款
    private static final String ASSIGNED_TASK_CONFIRM_URL = SERVER_ADDRESS + "/users/%s/assignments/%s/confirmation";
    public static JsonObjectRequest confirmAssignedTaskRequest(String assignId, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(assignId));

        return new CustomRequest(Request.Method.POST, String.format(ASSIGNED_TASK_CONFIRM_URL, SessionManager.getInstance().getUserId(), assignId), listener, errorListener);
    }

    // 任务详情
    private static final String TASK_DETAIL_INFO_URL = SERVER_ADDRESS + "/users/%s/assignments/%s";
    public static JsonObjectRequest taskDetailInfoRequest(String assignId, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(assignId));

        return new CustomRequest(Request.Method.GET, String.format(TASK_DETAIL_INFO_URL, SessionManager.getInstance().getUserId(), assignId), listener, errorListener);
    }

    // 获得接接口api过滤配置
    private static final String SPIDER_MATCH_CONFIGS_URL = SERVER_ADDRESS + "/spiderMatchConfigs";
    public static JsonObjectRequest spiderMatchConfigsRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new CustomRequest(Request.Method.GET, SPIDER_MATCH_CONFIGS_URL, listener, errorListener);
    }

    // 获取银行信息
    private static final String BANK_ACCOUNTS_URL = SERVER_ADDRESS + "/users/%s/bankAccounts?";
    public static JsonObjectRequest backAccountsRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        StringBuilder sb = new StringBuilder(String.format(BANK_ACCOUNTS_URL, SessionManager.getInstance().getUserId()));
        sb.append("bankType=3");

        return new CustomRequest(Request.Method.GET, sb.toString(), listener, errorListener);
    }

    // 提现
    private static final String WITHDRAW_URL = SERVER_ADDRESS + "/users/%s/capital/withdrawal?";
    public static JsonObjectRequest withdrawRequest(double withdrawAmount, String password, int bankAccountId, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(password));

        StringBuilder sb = new StringBuilder(String.format(WITHDRAW_URL, SessionManager.getInstance().getUserId()));
        sb.append("withdrawAmount=").append(String.format("%.1f", withdrawAmount))
                .append("&withdrawPassword=").append(password)
                .append("&bankAccountId=").append(bankAccountId);

        return new CustomRequest(Request.Method.POST, sb.toString(), listener, errorListener);
    }

    // 提现记录
    public static JsonObjectRequest withdrawHistoryRequest(int pageSize, String timeStamp, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        StringBuilder sb = new StringBuilder(String.format(WITHDRAW_URL, SessionManager.getInstance().getUserId()));

        if (pageSize <= 0) {
            sb.append("pageSize=").append(DEFAULT_TASK_LIST_PAGE_SIZE);
        } else {
            sb.append("pageSize=").append(pageSize);
        }

        if (timeStamp != null) {
            try {
                String encodedTimeStamp = URLEncoder.encode(timeStamp, "utf-8");
                sb.append("&timeStamp=").append(encodedTimeStamp);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return new CustomRequest(Request.Method.GET, sb.toString(), listener, errorListener);
    }

    // 排行榜
    private static final String RANKING_URL = SERVER_ADDRESS + "/statistics/rankingList";
    public static JsonObjectRequest rankingRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new CustomRequest(Request.Method.GET, RANKING_URL, listener, errorListener);
    }

    // 提交任务
    private static final String SUBMIT_TASK_URL = SERVER_ADDRESS + "/users/%s/assignments/%s/submission";
    public static JsonObjectRequest submitTaskRequest(String assignId, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        Assert.assertTrue(!TextUtils.isEmpty(assignId));

        String url = String.format(SUBMIT_TASK_URL, SessionManager.getInstance().getUserId(), assignId);
        return new CustomRequest(Request.Method.POST, url, listener, errorListener);
    }

    // 我的邀请
    private static final String INVITATION_RECORDS_URL = SERVER_ADDRESS + "/statistics/suboveriding?";
    public static JsonObjectRequest invitationRecordsRequest(int page, int pageSize, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        StringBuilder sb = new StringBuilder(INVITATION_RECORDS_URL);
        sb.append("page=").append(page);

        if (pageSize <= 0) {
            sb.append("&pageSize=").append(DEFAULT_TASK_LIST_PAGE_SIZE);
        } else {
            sb.append("&pageSize=").append(pageSize);
        }

        return new CustomRequest(Request.Method.GET, sb.toString(), listener, errorListener);
    }

    // 获取最新的版本号
    private static final String LATEST_CLIENT_VERSION_URL = SERVER_ADDRESS + "/platformInfos/andriod_client_version";
    public static JsonObjectRequest latestClientVersionRequest(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new CustomRequest(Request.Method.GET, LATEST_CLIENT_VERSION_URL, listener, errorListener);
    }

    // 获取新版的下载地址
    private static final String RELEASED_VERSIONS_URL = SERVER_ADDRESS + "/releasedVersions?";
    public static JsonObjectRequest releasedVersionsRequest(int versionCode, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        StringBuilder sb = new StringBuilder(RELEASED_VERSIONS_URL);
        sb.append("clientVersion=").append(versionCode)
                .append("&clientType=3");

        return new CustomRequest(Request.Method.GET, sb.toString(), listener, errorListener);
    }
}
