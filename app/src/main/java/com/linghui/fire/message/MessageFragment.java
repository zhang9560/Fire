package com.linghui.fire.message;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import com.linghui.fire.R;

/**
 * Created by Yanghai on 2015/9/29.
 */
public class MessageFragment extends PreferenceFragmentCompat {
    public static final String TAG = MessageFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.message);
    }
}
