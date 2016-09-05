package com.linghui.fire.settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.linghui.fire.R;

/**
 * Created by yhzhang on 2015/12/2.
 */
public class UserAgreementFragment extends Fragment {

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
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.user_agreement);
        mWebView.loadUrl("http://www.hu-xiu.com/clause-hongbao.html");
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
