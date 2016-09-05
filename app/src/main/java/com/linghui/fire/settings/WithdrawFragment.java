package com.linghui.fire.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import com.linghui.fire.R;
import com.linghui.fire.login.MobileVerifyActivity;
import com.linghui.fire.session.SessionManager;

/**
 * Created by Yanghai on 2015/9/30.
 */
public class WithdrawFragment extends PreferenceFragmentCompat {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_withdraw);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.withdraw);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(Contants.PREF_KEY_TENPAY_WITHDRAW)) {
            if (SessionManager.getInstance().getHasSetWithdrawPassword()) {
                Intent intent = new Intent(getContext(), GeneralActivity.class);
                intent.setAction(SubmitWithdrawFragment.class.getName());
                startActivity(intent);
            } else {
                new AlertDialog.Builder(getContext()).setTitle(R.string.very_important)
                        .setMessage(R.string.set_withdraw_password_tip)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(getContext(), MobileVerifyActivity.class);
                                intent.putExtra(MobileVerifyActivity.EXTRA_KEY_MODE, MobileVerifyActivity.MODE_RESET_WITHDRAW_PASSWORD);
                                startActivity(intent);
                            }
                        }).show();
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
