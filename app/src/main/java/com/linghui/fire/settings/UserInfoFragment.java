package com.linghui.fire.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionChangedEvent;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.R;
import com.linghui.fire.widget.CustomPreference;
import com.squareup.otto.Subscribe;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yanghai on 2015/9/30.
 */
public class UserInfoFragment extends PreferenceFragmentCompat {
    public static final String TAG = UserInfoFragment.class.getSimpleName();

    private static final int REQUEST_CODE_GET_AVATAR_BY_CAMERA = 1000;
    private static final int REQUEST_CODE_GET_AVATAR_BY_ALBUM = 1001;
    private static final int REQUEST_CODE_REAL_NAME_AUTH = 1003;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.user_info);
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_user_info);
        SessionManager.getInstance().getEventBus().register(this);
        SessionManager.getInstance().syncUserInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SessionManager.getInstance().getEventBus().unregister(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        if (!TextUtils.isEmpty(key)) {
            if (key.equals(Contants.PREF_KEY_AVATAR)) {
                new AlertDialog.Builder(getContext()).setItems(R.array.choose_avatar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        switch (which) {
                            case 0:
                                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                                startActivityForResult(intent, REQUEST_CODE_GET_AVATAR_BY_CAMERA);
                                break;
                            case 1:
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                startActivityForResult(intent, REQUEST_CODE_GET_AVATAR_BY_ALBUM);
                                break;
                        }
                    }
                }).show();
            } else if (key.equals(Contants.PREF_KEY_NICK_NAME)) {
                View view = LayoutInflater.from(getContext()).inflate(R.layout.pref_edittext, null);
                final EditText nickNameEdit = (EditText)(view.findViewById(R.id.edittext));

                new AlertDialog.Builder(getContext()).setView(view).setTitle(R.string.pref_title_nick_name)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    setNickName(nickNameEdit.getText().toString());
                                }
                            }
                        }).show();
            } else if (key.equals(Contants.PREF_KEY_REAL_NAME_AUTH)) {
                Intent intent = new Intent(getActivity(), GeneralActivity.class);
                intent.setAction(RealNameAuthFragment.class.getName());
                startActivityForResult(intent, REQUEST_CODE_REAL_NAME_AUTH);
            } else if (key.equals(Contants.PREF_KEY_BINDING)) {
                Intent intent = new Intent(getActivity(), GeneralActivity.class);
                intent.setAction(TaobaoAccountBindingFragment.class.getName());
                startActivity(intent);
            } else {
                return super.onPreferenceTreeClick(preference);
            }

            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_AVATAR_BY_ALBUM && resultCode == Activity.RESULT_OK) {
            mAvatarPref.setImage(data.getData());
        } else if (requestCode == REQUEST_CODE_GET_AVATAR_BY_CAMERA && resultCode == Activity.RESULT_OK) {

        }
    }

    @Subscribe
    public void onSessionChanged(SessionChangedEvent event) {
        setPreferenceData();
    }

    private void initPreferences() {
        PreferenceScreen ps = getPreferenceScreen();

        mAvatarPref = (CustomPreference)(ps.findPreference(Contants.PREF_KEY_AVATAR));
        mNickNamePref = (CustomPreference)(ps.findPreference(Contants.PREF_KEY_NICK_NAME));
        mMobileNumberPref = (CustomPreference)(ps.findPreference(Contants.PREF_KEY_USER_NAME));
        mInviteCodePref = (CustomPreference)(ps.findPreference(Contants.PREF_KEY_INVITE_CODE));
        mInviteCodePref.setShowNextImage(false);
        mRealNameAuthPref = (CustomPreference)(ps.findPreference(Contants.PREF_KEY_REAL_NAME_AUTH));
        mBindTaobaoPref = (CustomPreference)(findPreference(Contants.PREF_KEY_BINDING));
        mTenpayPref = (CustomPreference)(findPreference(Contants.PREF_KEY_TENPAY));

        CustomPreference accountSafetyPref = (CustomPreference)(findPreference(Contants.PREF_KEY_ACCOUNT_SAFETY));
        accountSafetyPref.setText(getString(R.string.pref_text_change_password));

        setPreferenceData();
    }

    private void setPreferenceData() {
        String avatarPath = SessionManager.getInstance().getAvatarPath();
        if (TextUtils.isEmpty(avatarPath)) {
            mAvatarPref.setImage(Uri.parse("res:///" + R.drawable.ic_pref_default_avatar));
        } else {
            mAvatarPref.setImage(Uri.parse(avatarPath));
        }

        mNickNamePref.setText(SessionManager.getInstance().getNickName());
        mInviteCodePref.setText(SessionManager.getInstance().getInviteCode());

        String mobileNumber = SessionManager.getInstance().getMobile();
        mMobileNumberPref.setText(mobileNumber.replace(mobileNumber.substring(3, 7), "****"));
        mMobileNumberPref.setShowNextImage(false);

        switch (SessionManager.getInstance().getRealNameAuthStatus()) {
            case SessionManager.REAL_NAME_AUTH_STATUS_NOT_IDENTIFIED:
                mRealNameAuthPref.setText(Html.fromHtml(getString(R.string.pref_text_not_auth)));
                break;
            case SessionManager.REAL_NAME_AUTH_STATUS_IN_REVIEW:
                mRealNameAuthPref.setText(Html.fromHtml(getString(R.string.pref_text_in_review)));
                break;
            case SessionManager.REAL_NAME_AUTH_STATUS_INDENTIFIED:
                mRealNameAuthPref.setText(getString(R.string.pref_text_already_auth));
                break;
            case SessionManager.REAL_NAME_AUTH_STATUS_REJECTED:
                mRealNameAuthPref.setText(Html.fromHtml(getString(R.string.pref_text_rejected)));
                break;
        }

        String taobaoAccount = SessionManager.getInstance().getTaobaoAccount();
        if (!TextUtils.isEmpty(taobaoAccount)) {
            mBindTaobaoPref.setText(taobaoAccount);
            mBindTaobaoPref.setShowNextImage(false);
        } else {
            mBindTaobaoPref.setText(Html.fromHtml(getString(R.string.pref_text_not_bind)));
        }

        mTenpayPref.setText(SessionManager.getInstance().getBankAccountNo());
    }

    private void setNickName(String nickName) {
        if (!TextUtils.isEmpty(nickName)) {
            final ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMessage(getString(R.string.sending));
            progressDialog.show();

            Map<String, String> params = new HashMap<String, String>();
            params.put("nickName", nickName);
            VolleyUtils.getInstance().sendRequest(API.submitUserInfoRequest(params, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "submitUserInfo : " + response);
                    progressDialog.dismiss();

                    if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
                        SessionManager.getInstance().syncUserInfo();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "submitUserInfo error : " + error);
                    progressDialog.dismiss();
                }
            }));
        }
    }

    private CustomPreference mAvatarPref;
    private CustomPreference mNickNamePref;
    private CustomPreference mMobileNumberPref;
    private CustomPreference mInviteCodePref;
    private CustomPreference mRealNameAuthPref;
    private CustomPreference mBindTaobaoPref;
    private CustomPreference mTenpayPref;
}
