package com.linghui.fire.task;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.linghui.fire.R;

/**
 * Created by yhzhang on 2015/12/8.
 */
public class AdverFragment extends Fragment {

    public static final String EXTRA_KEY_URL = "url";

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

                if (!TextUtils.isEmpty(view.getTitle())) {
                    getActivity().setTitle(view.getTitle());
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWebView.loadUrl(getActivity().getIntent().getStringExtra(EXTRA_KEY_URL));
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
