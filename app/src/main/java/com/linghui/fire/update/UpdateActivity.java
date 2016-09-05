package com.linghui.fire.update;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import com.linghui.fire.R;
import com.linghui.fire.session.SessionManager;

/**
 * Created by yhzhang on 2015/12/17.
 */
public class UpdateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            SessionManager.getInstance().getEventBus().post(new UpdateEvent(UpdateEvent.ACTION_CANCEL));
        }

        return super.onKeyUp(keyCode, event);
    }

    public void onCancelled(View view) {
        SessionManager.getInstance().getEventBus().post(new UpdateEvent(UpdateEvent.ACTION_CANCEL));
        finish();
    }

    public void onConfirmed(View view) {
        SessionManager.getInstance().getEventBus().post(new UpdateEvent(UpdateEvent.ACTION_UPDATE));
        finish();
    }
}
