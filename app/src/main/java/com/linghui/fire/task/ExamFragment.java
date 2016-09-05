package com.linghui.fire.task;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.session.SessionManager;

/**
 * Created by yhzhang on 2015/12/3.
 */
public class ExamFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_webview, null);
        mWebView = (WebView) rootView.findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                getActivity().setTitle(view.getTitle());
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.exam);

        String sessionCookie = SessionManager.getInstance().getSessionCookie();

        if (!TextUtils.isEmpty(sessionCookie)) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setCookie(API.SERVER_DOMAIN, sessionCookie);
            mWebView.loadUrl(API.SERVER_ADDRESS + "/examination/guide?testId=2");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mWebView.stopLoading();
        mWebView.removeAllViews();
        mWebView.destroy();
    }

    private WebView mWebView;
}
