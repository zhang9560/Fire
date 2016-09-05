package com.linghui.fire;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import com.linghui.fire.login.LoginActivity;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.update.UpdateService;


/**
 * Created by Yanghai on 2015/10/3.
 */
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (AppContext.getInstance().launched) {
            launch();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    launch();
                }
            }, 1500);
        }

        AppContext.getInstance().launched = true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void launch() {
        if (SessionManager.getInstance().restore()) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }

        Intent updateIntent = new Intent(this, UpdateService.class);
        updateIntent.setAction(UpdateService.ACTION_CHECK_LATEST_VERSION);
        startService(updateIntent);

        finish();
    }
}
