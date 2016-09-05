package com.linghui.fire.task;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.linghui.fire.R;
import com.linghui.fire.widget.SessionBaseActivity;

/**
 * Created by yhzhang on 2015/11/24.
 */
public class CourseViewActivity extends SessionBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_course);

        initViews();
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

    private void initViews() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
