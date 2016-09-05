package com.linghui.fire.task;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.widget.SessionBaseActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

/**
 * Created by yhzhang on 2015/11/24.
 */
public class TaskViewerActivity extends SessionBaseActivity {
    public static final String TAG = TaskViewerActivity.class.getSimpleName();

    public static final String EXTRA_KEY_TASK_ASSIGN_ID = "task_assign_id";
    public static final String EXTRA_KEY_TASK_URL = "task_url";
    public static final String EXTRA_KEY_TASK_TAGS = "task_tags";
    public static final String EXTRA_KEY_TASK_QUANTITY = "task_quantity";
    public static final String EXTRA_KEY_TASK_PRICE = "task_price";
    public static final String EXTRA_KEY_TASK_STATUS = "task_status";
    public static final String EXTRA_KEY_TASK_CATEGORY = "task_category";
    public static final String EXTRA_KEY_TASK_TARGET_ITEM_ID = "task_target_item_id";

    private static final String SHARED_PREFS_NAME = "task_tags_" + SessionManager.getInstance().getUserId();

    private static final int TIMEOUT_MILLISECONDS = 10000;

    private static final int MESSAGE_TIMEOUT_COUNTDOWN_START = 0;
    private static final int MESSAGE_TIMEOUT = 1;
    private static final int MESSAGE_PAGE_LOADED = 2;

    private static class UIHandler extends Handler {
        private WeakReference<TaskViewerActivity> mActivity;
        private boolean mTimeout = false;
        private boolean mPageLoaded = false;

        public UIHandler(TaskViewerActivity activity) {
            mActivity = new WeakReference<TaskViewerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final TaskViewerActivity theActivity = mActivity.get();

            if (theActivity != null) {
                if (msg.what == MESSAGE_TIMEOUT_COUNTDOWN_START) {
                    mTimeout = false;
                    mPageLoaded = false;

                    theActivity.mTimeoutTimer = new Timer("timeout_timer");
                    theActivity.mTimeoutTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            sendEmptyMessage(MESSAGE_TIMEOUT);
                        }
                    }, TIMEOUT_MILLISECONDS);

                    theActivity.mCheckOrdersWebView.loadUrl(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_ORDER_LIST_URL));
                } else if (msg.what == MESSAGE_TIMEOUT && !mPageLoaded) {
                    theActivity.mCheckOrdersWebView.stopLoading();
                    mTimeout = true;
                    removeMessages(MESSAGE_PAGE_LOADED);

                    theActivity.mProgressDialog.dismiss();
                    new AlertDialog.Builder(theActivity).setTitle(R.string.very_important)
                            .setMessage(R.string.not_found_order)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.still_submit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface Dialog, int which) {
                                    theActivity.submit();
                                }
                            }).show();
                } else if (msg.what == MESSAGE_PAGE_LOADED && !mTimeout) {
                    theActivity.mTimeoutTimer.cancel();
                    mPageLoaded = true;
                    removeMessages(MESSAGE_TIMEOUT);

                    boolean foundOrder = false;
                    String content = (String)msg.obj;

                    try {
                        String jsonContent = content.substring(content.indexOf("(") + 1, content.length() - 1);
                        JSONObject object = new JSONObject(jsonContent);
                        JSONObject data = object.getJSONObject("data").getJSONObject("data");
                        JSONArray group = data.getJSONArray("group");

                        for (int i = 0; i < group.length(); i++) {
                            JSONObject item = group.optJSONObject(i);
                            Iterator<String> cellGroupKeysIter = item.keys();

                            if (cellGroupKeysIter.hasNext()) {
                                JSONArray cellGroup = item.getJSONArray(cellGroupKeysIter.next());

                                JSONArray subAuctionIds = null;
                                String status = null;

                                for (int j = 0; j < cellGroup.length(); j++) {
                                    JSONObject cellItem = cellGroup.getJSONObject(j);
                                    String cellType = cellItem.getString("cellType");

                                    if (cellType.equals("storage")) {
                                        JSONArray cellData = cellItem.getJSONArray("cellData");

                                        for (int k = 0; k < cellData.length(); k++) {
                                            JSONObject cellDataItem = cellData.getJSONObject(k);
                                            String cellDataItemTag = cellDataItem.getString("tag");

                                            if (cellDataItemTag.equals("storage")) {
                                                JSONObject cellFields = cellDataItem.getJSONObject("fields");
                                                subAuctionIds = cellFields.getJSONArray("subAuctionIds");
                                                break;
                                            }
                                        }
                                    } else if (cellType.equals("head")) {
                                        JSONArray cellData = cellItem.getJSONArray("cellData");

                                        for (int k = 0; k < cellData.length(); k++) {
                                            JSONObject cellDataItem = cellData.getJSONObject(k);
                                            String cellDataItemTag = cellDataItem.getString("tag");

                                            if (cellDataItemTag.equals("status")) {
                                                JSONObject cellFields = cellDataItem.getJSONObject("fields");
                                                status = cellFields.getString("text");
                                                break;
                                            }
                                        }
                                    }

                                    if (subAuctionIds != null && !TextUtils.isEmpty(status)) {
                                        for (int k = 0; k < subAuctionIds.length(); k++) {
                                            String id = subAuctionIds.getString(k);

                                            if (id.equals(theActivity.mTaskTargetItemId) &&
                                                    (status.contains("买家已付款") || status.contains("卖家已发货"))) {
                                                foundOrder = true;
                                                throw new Exception("item found");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    } finally {
                        if (foundOrder) {
                            theActivity.submit();
                        } else {
                            theActivity.mProgressDialog.dismiss();
                            new AlertDialog.Builder(theActivity).setTitle(R.string.very_important)
                                    .setMessage(R.string.not_found_order)
                                    .setNegativeButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.still_submit, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface Dialog, int which) {
                                            theActivity.submit();
                                        }
                                    }).show();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_viewer);

        initData();
        initViews();
        getSpiderMatchConfigs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCheckOrdersWebView.stopLoading();
        mCheckOrdersWebView.removeAllViews();
        mCheckOrdersWebView.destroy();

        mWebView.stopLoading();
        mWebView.removeAllViews();
        mWebView.destroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void initData() {
        Intent intent = getIntent();
        mAssignId = intent.getStringExtra(EXTRA_KEY_TASK_ASSIGN_ID);
        mTaskUrl = intent.getStringExtra(EXTRA_KEY_TASK_URL);
        mTaskQuantity = intent.getIntExtra(EXTRA_KEY_TASK_QUANTITY, 0);
        mTaskPrice = intent.getDoubleExtra(EXTRA_KEY_TASK_PRICE, 0);
        mTaskStatus = intent.getIntExtra(EXTRA_KEY_TASK_STATUS, -1);
        mTaskCategory = intent.getIntExtra(EXTRA_KEY_TASK_CATEGORY, Task.TASK_CATEGORY_TRAFFIC);
        mTaskTargetItemId = intent.getStringExtra(EXTRA_KEY_TASK_TARGET_ITEM_ID);

        initTaskTags(intent.getStringExtra(EXTRA_KEY_TASK_TAGS));
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("");

        mProgressDialog = new ProgressDialog(TaskViewerActivity.this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage(getString(R.string.sending));

        initCheckOrdersWebView();

        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        mWebView = (WebView)findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String lowerCaseUrl = url.toLowerCase();
                if (lowerCaseUrl.startsWith("http://") || lowerCaseUrl.startsWith("https://")) {
                    view.loadUrl(url);
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    String decodedUrl = URLDecoder.decode(url, "utf-8");
                    Log.d(TAG, decodedUrl);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String title = view.getTitle();
                setTitle(title);

                if (title != null && title.equals("宝贝详情") && view.getProgress() == 100) {
                    // 页面加载完成后首先检测cookie的旺旺字段，发现不是绑定的旺旺则弹出提示框
                    // 告知更换旺旺，并刷新该页面，同时清除所有WebView的cookie
                    String cookie = cookieManager.getCookie(url);
                    Map<String, String> cookieMap = getCookieMap(cookie);

                    if (cookieMap != null) {
                        String taobaoAccount = cookieMap.get(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TAOBAO_LOGIN_COOKIE));

                        if (!TextUtils.isEmpty(taobaoAccount)) {
                            try {
                                String decodedAccount = URLDecoder.decode(taobaoAccount, "utf8");
                                if (!decodedAccount.equals(SessionManager.getInstance().getTaobaoAccount())) {
                                    new AlertDialog.Builder(TaskViewerActivity.this).setCancelable(false)
                                            .setTitle(R.string.very_important)
                                            .setMessage(R.string.taobao_account_not_match)
                                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    cookieManager.removeAllCookie();
                                                    mWebView.loadUrl(mTaskUrl);
                                                }
                                            }).show();
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                if (url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TAOBAO_SEND_MESSAGE)) ||
                        url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TMALL_SEND_MESSAGE))) {
                    setTaskTagCompleted(Tag.TAG_TYPE_CHAT);
                } else if (url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TAOBAO_ADD_COLLECT)) ||
                        url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TMALL_ADD_COLLECT))) {
                    setTaskTagCompleted(Tag.TAG_TYPE_ADD_TO_FAVORITES);
                } else if (url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TAOBAO_ADD_BAG)) ||
                        url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TMALL_ADD_BAG))) {
                    setTaskTagCompleted(Tag.TAG_TYPE_ADD_TO_CART);
                } else if (url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TAOBAO_RATES)) ||
                        url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TMALL_RATES))) {
                    setTaskTagCompleted(Tag.TAG_TYPE_VIEW_RATES);
                } else if (url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TAOBAO_BUILD_ORDER)) ||
                        url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TMALL_BUILD_ORDER))) {
                    if (mTaskCategory == Task.TASK_CATEGORY_TRAFFIC) {
                        new AlertDialog.Builder(TaskViewerActivity.this).setCancelable(false)
                                .setTitle(R.string.very_important).setMessage(R.string.commit_task_tip)
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mWebView.loadUrl(mTaskUrl);
                                    }
                                }).show();
                    }
                }
            }
        });

        TextView taskPriceText = (TextView)findViewById(R.id.task_price);
        taskPriceText.setText(Html.fromHtml(getString(R.string.task_price_format_string, mTaskQuantity, mTaskPrice)));

        mTaskBtn = (TextView)findViewById(R.id.task_btn);
        switch (mTaskStatus) {
            case AssignedTask.TASK_STATUS_ASSIGNED:
            case AssignedTask.TASK_STATUS_DOING:
                mTaskBtn.setText(R.string.commit_task);
                mTaskBtn.setEnabled(true);
                break;
            case AssignedTask.TASK_STATUS_SUBMITTED:
                mTaskBtn.setText(R.string.wait_for_refund);
                break;
            case AssignedTask.TASK_STATUS_CONFIRMING:
                mTaskBtn.setText(R.string.confirm_the_money);
                break;
            case AssignedTask.TASK_STATUS_COMPLETED:
                mTaskBtn.setText(R.string.task_completed);
                break;
            default:
                mTaskBtn.setText(R.string.task_cancelled);
                break;
        }
    }

    private void getSpiderMatchConfigs() {
        VolleyUtils.getInstance().sendRequest(API.spiderMatchConfigsRequest(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "spiderMatchConfigs : " + response);

                if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                    JSONArray result = response.optJSONArray("result");
                    SpiderMatchConfigs.INSTANCE.parse(result);
                }

                mWebView.loadUrl(mTaskUrl);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "spiderMatchConfigs error : " + error);

                mWebView.loadUrl(mTaskUrl);
            }
        }));
    }

    public void onTaskCommitted(View view) {
        if (isAllTagsCompleted()) {
            mProgressDialog.show();

            if (mTaskCategory == Task.TASK_CATEGORY_TRAFFIC) {
                submit();
            } else if (mTaskCategory == Task.TASK_CATEGORY_COMMISSION) {
                startTimeoutTimer();
            }
        } else {
            new AlertDialog.Builder(TaskViewerActivity.this).setTitle(R.string.heihei)
                    .setMessage(getString(R.string.not_all_tags_have_finished, getUncompletedTags()))
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        }
    }

    private Map<String, String> getCookieMap(String cookieString) {
        if (!TextUtils.isEmpty(cookieString)) {
            String[] cookieArray = Pattern.compile("; ").split(cookieString);

            if (cookieArray != null && cookieArray.length > 0) {
                Map<String, String> result = new HashMap<String, String>();

                for (String cookie : cookieArray) {
                    String[] pairs = cookie.split("=");
                    result.put(pairs[0], pairs[1]);
                }

                return result;
            }

            return null;
        }

        return null;
    }

    private void initTaskTags(String tags) {
        SharedPreferences sp = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        String savedTags = sp.getString(mAssignId, "");

        try {
            if (!TextUtils.isEmpty(savedTags)) {
                mTaskTags = new JSONObject(savedTags);
            } else {
                JSONArray array = new JSONArray(tags);
                mTaskTags = new JSONObject();

                for (int i = 0; i < array.length(); i++) {
                    Tag tag = Tag.fromJsonObject(array.optJSONObject(i));

                    if (tag != null) {
                        JSONObject item = new JSONObject();
                        item.put("tagName", tag.name);
                        item.put("completed", false);
                        mTaskTags.put(String.valueOf(tag.id), item);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setTaskTagCompleted(int tagId) {
        if (mTaskTags != null) {
            JSONObject obj = mTaskTags.optJSONObject(String.valueOf(tagId));

            if (obj != null) {
                try {
                    obj.put("completed", true);

                    SharedPreferences sp = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(mAssignId, mTaskTags.toString());
                    editor.commit();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isAllTagsCompleted() {
        if (mTaskTags != null && mTaskTags.length() > 0) {
            Iterator<String> keys = mTaskTags.keys();

            while (keys.hasNext()) {
                JSONObject obj = mTaskTags.optJSONObject(keys.next());

                if (!obj.optBoolean("completed")) {
                    return false;
                }
            }
        }

        return true;
    }

    private String getUncompletedTags() {
        StringBuilder uncompletedTags = new StringBuilder();
        Iterator<String> keys = mTaskTags.keys();

        while (keys.hasNext()) {
            JSONObject obj = mTaskTags.optJSONObject(keys.next());

            if (!obj.optBoolean("completed")) {
                uncompletedTags.append(obj.optString("tagName")).append(" ");
            }
        }

        return uncompletedTags.toString();
    }

    private void initCheckOrdersWebView() {
        mCheckOrdersWebView = (WebView)findViewById(R.id.check_orders_webview);
        mCheckOrdersWebView.getSettings().setJavaScriptEnabled(true);
        mCheckOrdersWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                if (url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TAOBAO_QUERY_BOUGHT_LIST)) ||
                        url.contains(SpiderMatchConfigs.INSTANCE.getConfig(SpiderMatchConfigs.CONFIG_ID_TMALL_QUERY_BOUGHT_LIST))) {
                    getOrderList(url);
                }
            }
        });
    }

    private void submit() {
        VolleyUtils.getInstance().sendRequest(API.submitTaskRequest(mAssignId, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "submitTask : " + response);
                mProgressDialog.dismiss();

                if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
                    if (mTaskCategory == Task.TASK_CATEGORY_TRAFFIC) {
                        SessionManager.getInstance().getEventBus().post(new TaskStatusChangedEvent(AssignedTask.TASK_STATUS_COMPLETED));
                    } else if (mTaskCategory == Task.TASK_CATEGORY_COMMISSION) {
                        SessionManager.getInstance().getEventBus().post(new TaskStatusChangedEvent(AssignedTask.TASK_STATUS_SUBMITTED));
                    }

                    SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE).edit();
                    editor.remove(mAssignId);
                    editor.commit();

                    SessionManager.getInstance().syncUserInfo();

                    Toast.makeText(TaskViewerActivity.this, getString(R.string.commit_success), Toast.LENGTH_SHORT).show();

                    setResult(RESULT_OK);
                    finish();
                } else {
                    new AlertDialog.Builder(TaskViewerActivity.this).setTitle(R.string.commit_failed)
                            .setMessage(response.optString("message")).setPositiveButton(R.string.confirm, null)
                            .show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "submitTask error : " + error);
                mProgressDialog.dismiss();
            }
        }));
    }

    private void getOrderList(String url) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(url);
        Map<String, String> cookieMap = getCookieMap(cookies);

        if (cookieMap != null) {
            URI uri = URI.create(url);
            CookieStore cookieStore = VolleyUtils.getInstance().getCookieManager().getCookieStore();
            Set<String> keySet = cookieMap.keySet();

            for (String key : keySet) {
                HttpCookie cookie = new HttpCookie(key, cookieMap.get(key));
                cookie.setDomain(uri.getHost());
                cookie.setPath("/");
                cookieStore.add(uri, cookie);
            }

            StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "getOrderList : " + response);

                    Message msg = new Message();
                    msg.what = MESSAGE_PAGE_LOADED;
                    msg.obj = response;
                    mHandler.sendMessage(msg);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "getOrderList error : " + error);
                }
            });

            request.setRetryPolicy(new DefaultRetryPolicy(5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyUtils.getInstance().sendRequest(request);
        }
    }

    private void startTimeoutTimer() {
        mHandler.sendEmptyMessage(MESSAGE_TIMEOUT_COUNTDOWN_START);
    }

    private WebView mCheckOrdersWebView;
    private WebView mWebView;
    private TextView mTaskBtn;
    private ProgressDialog mProgressDialog;

    private String mAssignId;
    private String mTaskUrl;
    private int mTaskQuantity;
    private double mTaskPrice;
    private JSONObject mTaskTags;
    private int mTaskStatus;
    private int mTaskCategory;
    private String mTaskTargetItemId;

    private UIHandler mHandler = new UIHandler(this);
    private Timer mTimeoutTimer;
}