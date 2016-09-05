package com.linghui.fire.task;

import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yhzhang on 2015/12/13.
 */
public enum  SpiderMatchConfigs {
    INSTANCE;

    public static final int CONFIG_ID_TAOBAO_SEND_MESSAGE = 1;
    public static final int CONFIG_ID_TMALL_SEND_MESSAGE = 2;
    public static final int CONFIG_ID_TAOBAO_ADD_COLLECT = 3;
    public static final int CONFIG_ID_TMALL_ADD_COLLECT = 4;
    public static final int CONFIG_ID_TAOBAO_ADD_BAG = 5;
    public static final int CONFIG_ID_TMALL_ADD_BAG = 6;
    public static final int CONFIG_ID_BIND_TAOBAO_ACCOUNT = 7;
    public static final int CONFIG_ID_TAOBAO_LOGIN_COOKIE = 8;
    public static final int CONFIG_ID_TAOBAO_BUILD_ORDER = 9;
    public static final int CONFIG_ID_TMALL_BUILD_ORDER = 10;
    public static final int CONFIG_ID_TAOBAO_QUERY_BOUGHT_LIST = 11;
    public static final int CONFIG_ID_TMALL_QUERY_BOUGHT_LIST = 12;
    public static final int CONFIG_ID_ORDER_LIST_URL = 13;
    public static final int CONFIG_ID_TAOBAO_RATES = 14;
    public static final int CONFIG_ID_TMALL_RATES = 15;

    public synchronized void parse(JSONArray configs) {
        mConfigs.clear();

        if (configs != null) {
            for (int i = 0; i < configs.length(); i++) {
                JSONObject item = configs.optJSONObject(i);
                mConfigs.add(new Pair<Integer, String>(item.optInt("configId"), item.optString("configValue")));
            }
        }
    }

    public synchronized String getConfig(int configId) {
        for (Pair<Integer, String> pair : mConfigs) {
            if (pair.first == configId) {
                return pair.second;
            }
        }

        return "";
    }

    private List<Pair<Integer, String>> mConfigs = new ArrayList<Pair<Integer, String>>();
}
