package com.linghui.fire.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.webkit.CookieManager;
import com.linghui.fire.R;
import com.linghui.fire.login.LoginActivity;
import com.linghui.fire.session.SessionManager;

/**
 * Created by Yanghai on 2015/9/30.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements DialogInterface.OnClickListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_settings);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        if (!TextUtils.isEmpty(key)) {
            if (key.equals(Contants.PREF_KEY_EXIT)) {
                new AlertDialog.Builder(getContext()).setMessage(R.string.confirm_to_exit)
                        .setPositiveButton(R.string.confirm, this)
                        .setNegativeButton(R.string.cancel, this).show();
                return true;
            }
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            exit();
        }
    }

    private void exit() {
        SessionManager.getInstance().remove();

        // 清理所有webview的cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
