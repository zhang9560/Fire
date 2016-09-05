package com.linghui.fire.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.settings.UserAgreementFragment;
import org.json.JSONObject;


/**
 * Created by Yanghai on 2015/9/28.
 */
public class MobileVerifyActivity extends AppCompatActivity implements TextWatcher, View.OnClickListener,
        Response.Listener<JSONObject>, Response.ErrorListener {
    public static final String TAG = MobileVerifyActivity.class.getSimpleName();

    private static final int MOBILE_NUMBER_LENGTH = 11;

    private static final int USER_REGISTER_REQUEST_CODE = 0;
    private static final int RESET_PASSWORD_REQUEST_CODE = 1;

    // 获取验证的模式
    public static final String EXTRA_KEY_MODE = "mode";
    public static final int MODE_REGISTER_USER = 0;
    public static final int MODE_RESET_LOGIN_PASSWORD = 1;
    public static final int MODE_RESET_WITHDRAW_PASSWORD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_verify);

        initData();
        initViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mContainer.getVisibility() == View.VISIBLE) {
                    setTitle(R.string.mobile_verify);
                    mContainer.setVisibility(View.GONE);
                } else {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view == mGetVerificationCodeBtn) {
            if (mMobileNumberEdit.getText().length() != MOBILE_NUMBER_LENGTH) {
                new AlertDialog.Builder(this).setMessage(R.string.input_correct_mobile_number)
                        .setNegativeButton(R.string.confirm, null).show();
                return;
            }

            JsonObjectRequest request = null;
            switch (mMode) {
                case MODE_REGISTER_USER:
                    request = API.getSMSCodeRequest(mMobileNumberEdit.getText().toString(), this, this);
                    break;
                case MODE_RESET_LOGIN_PASSWORD:
                    request = API.resetLoginPasswordSMSCodeRequest(mMobileNumberEdit.getText().toString(), this, this);
                    break;
                case MODE_RESET_WITHDRAW_PASSWORD:
                    request = API.resetWithdrawPasswordSMSCodeRequest(mMobileNumberEdit.getText().toString(), this, this);
                    break;
            }

            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setMessage(getString(R.string.sending));
            }
            mProgressDialog.show();

            VolleyUtils.getInstance().sendRequest(request);
        } else if (view == mAgreementText) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("user_agreement");

            if (fragment == null) {
                fragment = new UserAgreementFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.container, fragment, "user_agreement");
                transaction.commit();
            } else {
                setTitle(R.string.user_agreement);
            }

            mContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!TextUtils.isEmpty(mMobileNumberEdit.getText())) {
            mGetVerificationCodeBtn.setEnabled(true);
        } else {
            mGetVerificationCodeBtn.setEnabled(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "getSMSCode error : " + error);
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        new AlertDialog.Builder(this).setMessage(R.string.get_verification_code_error)
                .setNegativeButton(R.string.confirm, null).show();
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(TAG, "getSMSCode response : " + response.toString());
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        int status = response.optInt("status");
        if (status == API.RESPONSE_STATUS_CREATED) {
            Intent intent = new Intent();
            switch (mMode) {
                case MODE_REGISTER_USER:
                    intent.setClass(this, RegisterActivity.class);
                    intent.putExtra(RegisterActivity.KEY_MOBILE_NUMBER, mMobileNumberEdit.getText().toString());
                    startActivityForResult(intent, USER_REGISTER_REQUEST_CODE);
                    break;
                case MODE_RESET_LOGIN_PASSWORD:
                case MODE_RESET_WITHDRAW_PASSWORD:
                    intent.setClass(this, ResetPasswordActivity.class);
                    intent.putExtra(EXTRA_KEY_MODE, mMode);
                    intent.putExtra(ResetPasswordActivity.EXTRA_KEY_MOBILE_NUMBER, mMobileNumberEdit.getText().toString());
                    startActivityForResult(intent, RESET_PASSWORD_REQUEST_CODE);
                    break;
            }
        } else {
            new AlertDialog.Builder(this).setMessage(response.optString("message"))
                    .setPositiveButton(R.string.confirm, null).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == USER_REGISTER_REQUEST_CODE && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        } else if (requestCode == RESET_PASSWORD_REQUEST_CODE && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mContainer.getVisibility() == View.VISIBLE) {
            setTitle(R.string.mobile_verify);
            mContainer.setVisibility(View.GONE);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void initViews() {
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mGetVerificationCodeBtn = (Button)findViewById(R.id.get_verification_code);
        mGetVerificationCodeBtn.setOnClickListener(this);

        mMobileNumberEdit = (EditText)findViewById(R.id.phoneNumber);
        mMobileNumberEdit.addTextChangedListener(this);
        String mobileNumber = SessionManager.getInstance().getUserName();
        if (!TextUtils.isEmpty(mobileNumber)) {
            mMobileNumberEdit.setText(mobileNumber);
        }

        mAgreementText = (TextView) findViewById(R.id.show_agreement);
        mAgreementText.setOnClickListener(this);
        if (mMode == MODE_REGISTER_USER) {
            findViewById(R.id.agreement_layout).setVisibility(View.VISIBLE);
        }

        mContainer = (ViewGroup)findViewById(R.id.container);
    }

    private void initData() {
        mMode = getIntent().getIntExtra(EXTRA_KEY_MODE, MODE_REGISTER_USER);
    }

    private Toolbar mToolbar;
    private EditText mMobileNumberEdit;
    private TextView mAgreementText;
    private Button mGetVerificationCodeBtn;
    private ViewGroup mContainer;
    private ProgressDialog mProgressDialog;

    private int mMode = MODE_REGISTER_USER;
}
