package com.linghui.fire.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.linghui.fire.R;
import com.linghui.fire.widget.ShareFragment;

/**
 * Created by Yanghai on 2015/9/29.
 */
public class PersonalInfoFragment extends PreferenceFragmentCompat {
    public static final String TAG = PersonalInfoFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.personal_info_settings);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.share, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                ShareFragment shareFragment = new ShareFragment();
                shareFragment.show(getFragmentManager(), "share");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
