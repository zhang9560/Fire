package com.linghui.fire.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.Html;
import android.text.TextUtils;
import com.linghui.fire.R;
import com.linghui.fire.login.MobileVerifyActivity;
import com.linghui.fire.session.SessionChangedEvent;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.widget.CustomPreference;
import com.squareup.otto.Subscribe;

/**
 * Created by Yanghai on 2015/10/6.
 */
public class AccountSafetyFragment extends PreferenceFragmentCompat {
    public static final String TAG = AccountSafetyFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.account_safety);
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_account_safety);
        SessionManager.getInstance().getEventBus().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SessionManager.getInstance().getEventBus().unregister(this);
    }

    @Subscribe
    public void onSessionChanged(SessionChangedEvent event) {
        setPreferenceData();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        if (!TextUtils.isEmpty(key)) {
            if (key.equals(Contants.PREF_KEY_RESET_LOGIN_PASSWORD)) {
                Intent intent = new Intent(getContext(), MobileVerifyActivity.class);
                intent.putExtra(MobileVerifyActivity.EXTRA_KEY_MODE, MobileVerifyActivity.MODE_RESET_LOGIN_PASSWORD);
                startActivity(intent);
            } else if (key.equals(Contants.PREF_KEY_RESET_WITHDRAW_PASSWORD)) {
                Intent intent = new Intent(getContext(), MobileVerifyActivity.class);
                intent.putExtra(MobileVerifyActivity.EXTRA_KEY_MODE, MobileVerifyActivity.MODE_RESET_WITHDRAW_PASSWORD);
                startActivity(intent);
            } else {
                return super.onPreferenceTreeClick(preference);
            }

            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void initPreferences() {
        CustomPreference resetLoginPwdPref = (CustomPreference)findPreference(Contants.PREF_KEY_RESET_LOGIN_PASSWORD);
        resetLoginPwdPref.setText(getString(R.string.pref_text_modify));
        mResetWithdrawPwdPref = (CustomPreference)findPreference(Contants.PREF_KEY_RESET_WITHDRAW_PASSWORD);

        setPreferenceData();
    }

    private void setPreferenceData() {
        if (SessionManager.getInstance().getHasSetWithdrawPassword()) {
            mResetWithdrawPwdPref.setText(getString(R.string.pref_text_modify));
        } else {
            mResetWithdrawPwdPref.setText(Html.fromHtml(getString(R.string.pref_text_not_set)));
        }
    }

    private CustomPreference mResetWithdrawPwdPref;
}
