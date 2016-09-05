package com.linghui.fire.session;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by yhzhang on 2015/11/21.
 */
public class SessionService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

    private Authenticator mAuthenticator;
}
