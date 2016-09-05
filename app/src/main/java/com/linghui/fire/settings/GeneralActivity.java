package com.linghui.fire.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import com.linghui.fire.R;
import com.linghui.fire.widget.SessionBaseActivity;

/**
 * Created by Yanghai on 2015/9/30.
 */
public class GeneralActivity extends SessionBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (!TextUtils.isEmpty(action)) {
            try {
                Class<? extends Fragment> fragment = (Class<? extends Fragment>) Class.forName(action);
                getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment.newInstance()).commit();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
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
}
