package com.linghui.fire.settings;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionChangedEvent;
import com.linghui.fire.session.SessionManager;
import com.squareup.otto.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by yhzhang on 2015/11/28.
 */
public class SubmitWithdrawFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = SubmitWithdrawFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_submit_withdraw, null);
        mModifyBindingView = rootView.findViewById(R.id.modify_binding);
        mModifyBindingView.setOnClickListener(this);
        mWithdrawTipText = (TextView)rootView.findViewById(R.id.withdraw_tip);
        mWithdrawTipText.setText(getString(R.string.withdraw_tip, SessionManager.getInstance().getAvailablePoints()));
        mWithdrawEdit = (EditText)rootView.findViewById(R.id.withdraw_edit);
        mSubmitBtn = (Button)rootView.findViewById(R.id.submit);
        mSubmitBtn.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.submit);
        SessionManager.getInstance().getEventBus().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SessionManager.getInstance().getEventBus().unregister(this);
    }

    @Override
    public void onClick(View view) {
        if (view == mModifyBindingView) {
            Intent intent = new Intent(getContext(), GeneralActivity.class);
            intent.setAction(TenpayFragment.class.getName());
            startActivity(intent);
        } else if (view == mSubmitBtn) {
            if (!TextUtils.isEmpty(mWithdrawEdit.getText())) {
                mWithdrawAmount = Double.valueOf(mWithdrawEdit.getText().toString());

                if (mWithdrawAmount > SessionManager.getInstance().getAvailablePoints()) {
                    new AlertDialog.Builder(getContext()).setMessage(R.string.not_enough_points).setPositiveButton(R.string.confirm, null).show();
                } else if (mWithdrawAmount <= 0) {
                    new AlertDialog.Builder(getContext()).setMessage(R.string.input_wrong_points).setPositiveButton(R.string.confirm, null).show();
                } else {
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.input_withdraw_password_layout, null);
                    TextView availablePointsText = (TextView)dialogView.findViewById(R.id.available_points);
                    availablePointsText.setText(getString(R.string.withdraw_password_dialog_message, mWithdrawAmount));
                    final EditText passwordEdit = (EditText)dialogView.findViewById(R.id.password);

                    new AlertDialog.Builder(getContext()).setTitle(R.string.input_withdraw_password).setCancelable(false)
                            .setView(dialogView)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mProgressDialog == null) {
                                        mProgressDialog = new ProgressDialog(getContext());
                                        mProgressDialog.setCancelable(false);
                                        mProgressDialog.setCanceledOnTouchOutside(false);
                                        mProgressDialog.setMessage(getString(R.string.sending));
                                    }
                                    mProgressDialog.show();

                                    mPassword = passwordEdit.getText().toString();
                                    VolleyUtils.getInstance().sendRequest(API.backAccountsRequest(mBankAccountsResponseListener, mBankAccountsErrorListener));
                                }
                            }).show();
                }
            }
        }
    }

    @Subscribe
    public void onSesseionChanged(SessionChangedEvent event) {
        mWithdrawTipText.setText(getString(R.string.withdraw_tip, SessionManager.getInstance().getAvailablePoints()));
    }

    private Response.Listener<JSONObject> mBankAccountsResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "backAccounts : " + response);

            if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                JSONArray result = response.optJSONArray("result");

                if (result != null) {
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject obj = result.optJSONObject(i);
                        String accountNo = obj.optString("accountNo");
                        String accountName = obj.optString("accountName");

                        if (SessionManager.getInstance().getBankAccountNo().equals(accountNo) &&
                                SessionManager.getInstance().getBankAccountName().equals(accountName)) {
                            int bankAccountId = obj.optInt("bankAccountId");
                            VolleyUtils.getInstance().sendRequest(API.withdrawRequest(mWithdrawAmount, mPassword, bankAccountId,
                                    mWithdrawResponseListener, mWithdrawErrorListener));
                            break;
                        }
                    }
                }
            } else {
                mProgressDialog.dismiss();
                Snackbar.make(getView(), response.optString("message"), Snackbar.LENGTH_SHORT).show();
            }
        }
    };

    private Response.ErrorListener mBankAccountsErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "backAccounts error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private Response.Listener<JSONObject> mWithdrawResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "withdraw : " + response);
            mProgressDialog.dismiss();

            if (response.optInt("status") != API.RESPONSE_STATUS_CREATED) {
                new AlertDialog.Builder(getContext()).setMessage(response.optJSONObject("result").optString("errorMessage"))
                        .setPositiveButton(R.string.confirm, null).show();
            } else {
                SessionManager.getInstance().syncUserInfo();
                Snackbar.make(getView(), R.string.withdraw_success, Snackbar.LENGTH_SHORT).show();
            }
        }
    };

    private Response.ErrorListener mWithdrawErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "withdraw error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private ProgressDialog mProgressDialog;

    private View mModifyBindingView;
    private TextView mWithdrawTipText;
    private EditText mWithdrawEdit;
    private Button mSubmitBtn;

    private double mWithdrawAmount;
    private String mPassword;
}
