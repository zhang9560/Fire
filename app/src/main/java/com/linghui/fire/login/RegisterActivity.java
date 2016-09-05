package com.linghui.fire.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.linghui.fire.MainActivity;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import org.json.JSONObject;

import java.lang.ref.WeakReference;


/**
 * Created by Yanghai on 2015/10/2.
 */
public class RegisterActivity extends AppCompatActivity implements TextWatcher, View.OnClickListener,
        Response.Listener<JSONObject>, Response.ErrorListener {
    public static final String TAG = RegisterActivity.class.getSimpleName();
    public static final String KEY_MOBILE_NUMBER = "mobile_number";

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 18;
    private static final int INVITE_CODE_LENGTH = 8;

    private static final int MESSAGE_COUNTDOWN = 1000;

    private static class UIHandler extends Handler {
        private WeakReference<RegisterActivity> mActivity;

        public UIHandler(RegisterActivity activity) {
            mActivity = new WeakReference<RegisterActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_COUNTDOWN && mActivity.get() != null) {
                mActivity.get().mResendVerificationCodeBtn.setEnabled(false);
                mActivity.get().mCountdownText.setText(mActivity.get().getString(R.string.sms_receiving_countdown, msg.arg1));

                if (msg.arg1 > 0) {
                    Message countDownMsg = new Message();
                    countDownMsg.what = MESSAGE_COUNTDOWN;
                    countDownMsg.arg1 = msg.arg1 - 1;
                    sendMessageDelayed(countDownMsg, 1000);
                } else {
                    mActivity.get().mResendVerificationCodeBtn.setEnabled(true);
                    mActivity.get().mCountdownText.setText("");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initData();

        Message msg = new Message();
        msg.what = MESSAGE_COUNTDOWN;
        msg.arg1 = 60;
        mHandler.sendMessage(msg);
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
        Log.d(TAG, "userRegister error : " + error);

        mProgressDialog.dismiss();

        new AlertDialog.Builder(this).setMessage(R.string.register_user_failed)
                .setNegativeButton(R.string.confirm, null).show();
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(TAG, "userRegister response : " + response);

        int status = response.optInt("status");
        if (status == API.RESPONSE_STATUS_CREATED) {
            mProgressDialog.dismiss();

            new AlertDialog.Builder(RegisterActivity.this).setMessage(R.string.register_user_success)
                    .setCancelable(false)
                    .setTitle(R.string.congratulations)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            login();
                        }
                    }).show();
        } else {
            mProgressDialog.dismiss();

            new AlertDialog.Builder(this).setMessage(response.optString("message"))
                    .setNegativeButton(R.string.confirm, null).show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mSubmitBtn) {
            final String verificationCode = mVerificationCodeEdit.getText().toString();
            final String password = mPasswordEdit.getText().toString();
            final String inviteCode = mInviteCodeEdit.getText().toString();

            if (verificationCode.length() != VERIFICATION_CODE_LENGTH) {
                new AlertDialog.Builder(this).setMessage(R.string.input_correct_verification_code)
                        .setPositiveButton(R.string.confirm, null).show();
                return;
            }

            if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
                new AlertDialog.Builder(this).setMessage(R.string.input_correct_login_password)
                        .setPositiveButton(R.string.confirm, null).show();
                return;
            }

            if (inviteCode.length() > 0 && inviteCode.length() != INVITE_CODE_LENGTH) {
                new AlertDialog.Builder(this).setMessage(R.string.input_correct_invite_code)
                        .setPositiveButton(R.string.confirm, null).show();
                return;
            } else if (inviteCode.length() == 0) {
                new AlertDialog.Builder(this).setMessage(R.string.input_invite_code_tip)
                        .setNegativeButton(R.string.have_invite_code, null)
                        .setPositiveButton(R.string.still_register, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                register(verificationCode, password, inviteCode);
                            }
                        }).show();
            } else {
                register(verificationCode, password, inviteCode);
            }
        } else if (view == mResendVerificationCodeBtn) {
            mResendVerificationCodeBtn.setEnabled(false);
            Message msg = new Message();
            msg.what = MESSAGE_COUNTDOWN;
            msg.arg1 = 60;
            mHandler.sendMessage(msg);

            mProgressDialog.show();
            VolleyUtils.getInstance().sendRequest(API.getSMSCodeRequest(mMobileNumber.toString(), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mProgressDialog.dismiss();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mProgressDialog.dismiss();
                }
            }));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!TextUtils.isEmpty(mVerificationCodeEdit.getText())
                && !TextUtils.isEmpty(mPasswordEdit.getText())
                && !TextUtils.isEmpty(mMobileNumber)) {
            mSubmitBtn.setEnabled(true);
        } else {
            mSubmitBtn.setEnabled(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private void initViews() {
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mVerificationCodeEdit = (EditText)findViewById(R.id.verification_code);
        mVerificationCodeEdit.addTextChangedListener(this);
        mPasswordEdit = (EditText)findViewById(R.id.password);
        mPasswordEdit.addTextChangedListener(this);
        mInviteCodeEdit = (EditText)findViewById(R.id.invite_code);
        mSubmitBtn = (Button)findViewById(R.id.submit);
        mSubmitBtn.setOnClickListener(this);
        mResendVerificationCodeBtn = (Button)findViewById(R.id.resend_verification_code);
        mResendVerificationCodeBtn.setOnClickListener(this);
        mCountdownText = (TextView)findViewById(R.id.countdown);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage(getString(R.string.sending));
    }

    private void initData() {
        Intent intent = getIntent();
        mMobileNumber = intent.getStringExtra(KEY_MOBILE_NUMBER);
    }

    private void register(String verificationCode, String password, String inviteCode) {
        mProgressDialog.show();

        JsonObjectRequest request = API.userRegisterRequest(mMobileNumber, verificationCode, password, getMacAddress(), inviteCode, this, this);
        VolleyUtils.getInstance().sendRequest(request);
    }

    private void login() {
        mProgressDialog.show();

        VolleyUtils.getInstance().sendRequest(API.userLoginRequest(mMobileNumber, mPasswordEdit.getText().toString(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "userLogin response : " + response);

                        mProgressDialog.dismiss();

                        int status = response.optInt("status");
                        if (status == API.RESPONSE_STATUS_CREATED) {
                            JSONObject result = response.optJSONObject("result");
                            if (result != null) {
                                SessionManager.getInstance().create(result.optString("userId"), result.optString("userName"), mPasswordEdit.getText().toString());

                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                startActivity(intent);
                                setResult(RESULT_OK);
                                finish();
                            }
                        } else {
                            new AlertDialog.Builder(RegisterActivity.this).setMessage(R.string.login_failed)
                                    .setPositiveButton(R.string.confirm, null).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "userLogin error : " + error);

                        mProgressDialog.dismiss();

                        new AlertDialog.Builder(RegisterActivity. this).setMessage(R.string.login_failed)
                                .setPositiveButton(R.string.confirm, null).show();
                    }
                }));
    }

    private String getMacAddress() {
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        return wifiManager.getConnectionInfo().getMacAddress();
    }

    private UIHandler mHandler = new UIHandler(this);

    private Toolbar mToolbar;
    private EditText mVerificationCodeEdit;
    private EditText mPasswordEdit;
    private EditText mInviteCodeEdit;
    private Button mSubmitBtn;
    private Button mResendVerificationCodeBtn;
    private TextView mCountdownText;

    private ProgressDialog mProgressDialog;

    private String mMobileNumber;
}
