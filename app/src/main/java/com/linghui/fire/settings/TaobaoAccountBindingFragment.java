package com.linghui.fire.settings;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.utils.TaskInfoUtils;
import org.json.JSONObject;

/**
 * Created by Yanghai on 2015/10/7.
 */
public class TaobaoAccountBindingFragment extends Fragment {
    public static final String TAG = TaobaoAccountBindingFragment.class.getSimpleName();

    private class InJavaScriptLocalObj implements DialogInterface.OnClickListener, Response.Listener<JSONObject>, Response.ErrorListener {

        @JavascriptInterface
        public void getTaobaoAccount(String account) {
            if (!TextUtils.isEmpty(account)) {
                mAccount = account;

                new AlertDialog.Builder(getContext()).setCancelable(false)
                        .setTitle(R.string.very_important)
                        .setMessage(getString(R.string.binding_information, mAccount))
                        .setPositiveButton(R.string.confirm_binding, this)
                        .setNegativeButton(R.string.change_binding_account, null).show();
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.binding_failed_title)
                        .setMessage(R.string.binding_failed_content)
                        .setPositiveButton(R.string.binding_failed_ok, null).show();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(getContext());
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialog.setMessage(getString(R.string.sending));
                }
                mProgressDialog.show();

                VolleyUtils.getInstance().sendRequest(API.bindingThirdpartiesRequest(mAccount, this, this));
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "bindThridParties error : " + error);

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }

        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "bindThridParties response : " + response);

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
                SessionManager.getInstance().syncUserInfo();
                getActivity().finish();
            } else {
                JSONObject result = response.optJSONObject("result");

                if (result != null) {
                    new AlertDialog.Builder(getContext())
                            .setMessage(result.optString("errorMessage"))
                            .setPositiveButton(R.string.binding_failed_ok, null).show();
                } else {
                    new AlertDialog.Builder(getContext())
                            .setMessage(response.optString("message"))
                            .setPositiveButton(R.string.binding_failed_ok, null).show();
                }
            }
        }

        private String mAccount;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHasBound = !TaskInfoUtils.isEmptyValue(SessionManager.getInstance().getTaobaoAccount());

        if (!mHasBound) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, null);
        mWebView = (WebView)view.findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        mWebView.setSaveEnabled(false);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                getActivity().setTitle(view.getTitle());
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_binding);

        if (mHasBound) {
            mWebView.loadUrl("https://h5.m.taobao.com/mlapp/mytaobao.html");
        } else {
            mWebView.loadUrl("https://login.taobao.com");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ViewGroup)getView().findViewById(R.id.root)).removeView(mWebView);
        mWebView.stopLoading();
        mWebView.removeAllViews();
        mWebView.destroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.binding, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.binding:
                mWebView.loadUrl("javascript:local_obj.getTaobaoAccount(" +
                        "document.getElementById(\"mtb-nickname\") != null ? document.getElementById(\"mtb-nickname\").value : null)");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private WebView mWebView;
    private ProgressDialog mProgressDialog;
    private boolean mHasBound;
}
