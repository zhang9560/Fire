package com.linghui.fire.login;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
 * Created by Yanghai on 2015/10/4.
 */
public class ResetPasswordActivity extends AppCompatActivity implements TextWatcher, View.OnClickListener,
        Response.Listener<JSONObject>, Response.ErrorListener{
    public static final String TAG = RegisterActivity.class.getSimpleName();

    public static final String EXTRA_KEY_MOBILE_NUMBER = "mobile_number";

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int LOGIN_PASSWORD_MIN_LENGTH = 6;
    private static final int LOGIN_PASSWORD_MAX_LENGTH = 18;
    private static final int WITHDRAW_PASSWORD_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        initData();
        initViews();
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
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "resetPassword error : " + error);

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        new AlertDialog.Builder(this).setMessage(R.string.reset_password_failed).setPositiveButton(R.string.confirm, null).show();
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(TAG, "resetPassword response : " + response);

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
            if (mMode == MobileVerifyActivity.MODE_RESET_WITHDRAW_PASSWORD) {
                SessionManager.getInstance().syncUserInfo();
            }

            new AlertDialog.Builder(this).setMessage(R.string.reset_password_succeed).setCancelable(false)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    }).show();
        } else {
            new AlertDialog.Builder(this).setMessage(R.string.reset_password_failed).setPositiveButton(R.string.confirm, null).show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mSubmitBtn) {
            String verificationCode = mVerificationCodeEdit.getText().toString();
            String password = mMode == MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD ?
                    mPasswordEdit.getText().toString() : mPasswordEdit2.getText().toString();

            if (verificationCode.length() != VERIFICATION_CODE_LENGTH) {
                new AlertDialog.Builder(this).setMessage(R.string.input_correct_verification_code)
                        .setPositiveButton(R.string.confirm, null).show();
                return;
            }

            if (mMode == MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD && (password.length() < LOGIN_PASSWORD_MIN_LENGTH || password.length() > LOGIN_PASSWORD_MAX_LENGTH)) {
                new AlertDialog.Builder(this).setMessage(R.string.input_correct_login_password)
                        .setPositiveButton(R.string.confirm, null).show();
                return;
            } else if (mMode == MobileVerifyActivity.MODE_RESET_WITHDRAW_PASSWORD && password.length() != WITHDRAW_PASSWORD_LENGTH) {
                new AlertDialog.Builder(this).setMessage(R.string.input_correct_withdraw_password)
                        .setPositiveButton(R.string.confirm, null).show();
                return;
            }

            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setMessage(getString(R.string.sending));
            }
            mProgressDialog.show();

            switch (mMode) {
                case MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD:
                    VolleyUtils.getInstance().sendRequest(API.resetLoginPasswordRequest(mMobileNumber, verificationCode, password, this, this));
                    break;
                case MobileVerifyActivity.MODE_RESET_WITHDRAW_PASSWORD:
                    VolleyUtils.getInstance().sendRequest(API.resetWithdrawPasswordRequest(verificationCode, password, this, this));
                    break;
                default:
                    mProgressDialog.dismiss();
            }

        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        EditText passwordEdit = mMode == MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD ? mPasswordEdit : mPasswordEdit2;

        if (!TextUtils.isEmpty(mVerificationCodeEdit.getText())
                && !TextUtils.isEmpty(passwordEdit.getText())) {
            mSubmitBtn.setEnabled(true);
        } else {
            mSubmitBtn.setEnabled(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private void initData() {
        mMobileNumber = getIntent().getStringExtra(EXTRA_KEY_MOBILE_NUMBER);
        mMode = getIntent().getIntExtra(MobileVerifyActivity.EXTRA_KEY_MODE, MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD);
    }

    private void initViews() {
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mVerificationCodeEdit = (EditText)findViewById(R.id.verification_code);
        mVerificationCodeEdit.addTextChangedListener(this);
        mPasswordEdit = (EditText)findViewById(R.id.password);
        mPasswordEdit.addTextChangedListener(this);
        mPasswordEdit2 = (EditText)findViewById(R.id.password2);
        mPasswordEdit2.addTextChangedListener(this);
        mSubmitBtn = (Button)findViewById(R.id.submit);
        mSubmitBtn.setOnClickListener(this);

        switch (mMode) {
            case MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD:
                setTitle(R.string.set_login_password);
                break;
            case MobileVerifyActivity.MODE_RESET_WITHDRAW_PASSWORD:
                setTitle(R.string.set_payment_password);
                mPasswordEdit.setVisibility(View.GONE);
                mPasswordEdit2.setVisibility(View.VISIBLE);
                break;
        }
    }

    private Toolbar mToolbar;
    private EditText mVerificationCodeEdit;
    private EditText mPasswordEdit;
    private EditText mPasswordEdit2;
    private Button mSubmitBtn;

    private int mMode = MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD;
    private String mMobileNumber;

    private ProgressDialog mProgressDialog;
}
