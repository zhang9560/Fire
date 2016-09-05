package com.linghui.fire.settings;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import org.json.JSONObject;

/**
 * Created by yhzhang on 2015/11/14.
 */
public class TenpayFragment extends Fragment implements Response.Listener<JSONObject>, Response.ErrorListener {
    public static final String TAG = TenpayFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tenpay, null);

        mSubmitBtn = (Button)rootView.findViewById(R.id.confirm);
        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getContext()).setTitle(R.string.very_important)
                        .setMessage(R.string.bind_tenpay_tip)
                        .setNegativeButton(R.string.wrong, null)
                        .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mProgressDialog.show();
                                VolleyUtils.getInstance().sendRequest(API.bindingBankAccountsRequest(
                                        API.BANK_ACCOUNT_ID_TENPAY,
                                        mTenpayNameEdit.getText().toString(),
                                        mTenpayAccountEdit.getText().toString(),
                                        TenpayFragment.this, TenpayFragment.this));
                            }
                        }).show();
            }
        });

        mTenpayNameEdit = (EditText)rootView.findViewById(R.id.tenpay_name_edit);
        mTenpayAccountEdit = (EditText)rootView.findViewById(R.id.tenpay_account_edit);
        mTenpayNameEdit.addTextChangedListener(mTextWatcher);
        mTenpayAccountEdit.addTextChangedListener(mTextWatcher);
        mTenpayNameEdit.setText(SessionManager.getInstance().getBankAccountName());
        mTenpayAccountEdit.setText(SessionManager.getInstance().getBankAccountNo());

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage(getString(R.string.sending));

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.tenpay);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "bindingBankAccountsRequest error : " + error);
        mProgressDialog.dismiss();
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(TAG, "bindingBankAccountsRequest : " + response);
        mProgressDialog.dismiss();

        if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
            SessionManager.getInstance().syncUserInfo();
            getActivity().finish();
        } else {
            new AlertDialog.Builder(getContext()).setTitle(R.string.tenpay).setMessage(response.optString("message"))
                    .setPositiveButton(R.string.confirm, null).show();
        }
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (!TextUtils.isEmpty(mTenpayNameEdit.getText()) && !TextUtils.isEmpty(mTenpayAccountEdit.getText())) {
                mSubmitBtn.setEnabled(true);
            } else {
                mSubmitBtn.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private EditText mTenpayNameEdit;
    private EditText mTenpayAccountEdit;
    private Button mSubmitBtn;
    private ProgressDialog mProgressDialog;
}
