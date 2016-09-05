package com.linghui.fire.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.MainActivity;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import org.json.JSONObject;

/**
 * Created by Yanghai on 2015/9/28.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener,
        Response.Listener<JSONObject>, Response.ErrorListener {
    public static final String TAG = LoginActivity.class.getSimpleName();

    private static final int MOBILE_VERIFICATION_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
    }

    @Override
    public void onClick(View view) {
        if (view == mLoginBtn) {
            login();
        } else if (view == mRegisterBtn) {
            Intent intent = new Intent(this, MobileVerifyActivity.class);
            intent.putExtra(MobileVerifyActivity.EXTRA_KEY_MODE, MobileVerifyActivity.MODE_REGISTER_USER);
            startActivityForResult(intent, MOBILE_VERIFICATION_REQUEST_CODE);
        } else if (view == mResetPasswordBtn) {
            Intent intent = new Intent(this, MobileVerifyActivity.class);
            intent.putExtra(MobileVerifyActivity.EXTRA_KEY_MODE, MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MOBILE_VERIFICATION_REQUEST_CODE && resultCode == RESULT_OK) {
            finish();
        }
    }


    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "userLogin error : " + error);

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        new AlertDialog.Builder(this).setMessage(R.string.login_failed)
                .setPositiveButton(R.string.confirm, null).show();
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(TAG, "userLogin response : " + response);

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
            JSONObject result = response.optJSONObject("result");
            if (result != null) {
                SessionManager.getInstance().create(result.optString("userId"), result.optString("userName"), mPasswordEdit.getText().toString());

                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } else {
            new AlertDialog.Builder(this).setMessage(response.optString("message"))
                    .setPositiveButton(R.string.confirm, null).show();
        }
    }

    void initViews() {
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mUserNameEdit = (EditText)findViewById(R.id.username);
        mPasswordEdit = (EditText)findViewById(R.id.password);
        mLoginBtn = (Button)findViewById(R.id.login);
        mLoginBtn.setOnClickListener(this);
        mRegisterBtn = (Button)findViewById(R.id.register);
        mRegisterBtn.setOnClickListener(this);
        mResetPasswordBtn = (Button)findViewById(R.id.reset_password);
        mResetPasswordBtn.setOnClickListener(this);
    }

    private void login() {
        String userName = mUserNameEdit.getText().toString();
        String password = mPasswordEdit.getText().toString();

        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(password)) {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setMessage(getString(R.string.now_login));
            }
            mProgressDialog.show();

            VolleyUtils.getInstance().sendRequest(API.userLoginRequest(userName, password, this, this));
        }
    }

    private Toolbar mToolbar;
    private EditText mUserNameEdit;
    private EditText mPasswordEdit;
    private Button mLoginBtn;
    private Button mRegisterBtn;
    private Button mResetPasswordBtn;

    private ProgressDialog mProgressDialog;
}
