package com.linghui.fire.server;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.linghui.fire.AppContext;

import java.net.CookieHandler;
import java.net.CookieManager;

/**
 * Created by Yanghai on 2015/10/2.
 */
public class VolleyUtils {

    private VolleyUtils() {
        mCookieManager = new CookieManager();
        CookieHandler.setDefault(mCookieManager);
        mRequestQueue = Volley.newRequestQueue(AppContext.getInstance());
    }

    public static VolleyUtils getInstance() {
        if (sInstance == null) {
            synchronized (VolleyUtils.class) {
                if (sInstance == null) {
                    sInstance = new VolleyUtils();
                }
            }
        }

        return sInstance;
    }

    public void sendRequest(Request request) {
        mRequestQueue.add(request);
    }

    public CookieManager getCookieManager() {
        return mCookieManager;
    }

    private volatile static VolleyUtils sInstance;

    private RequestQueue mRequestQueue;
    private CookieManager mCookieManager;
}
