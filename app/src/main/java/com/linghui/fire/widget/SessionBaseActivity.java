package com.linghui.fire.widget;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import com.linghui.fire.login.LoginActivity;
import com.linghui.fire.session.SessionManager;

/**
 * Created by yhzhang on 2015/11/22.
 */
public class SessionBaseActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();

        // 检测应用的account是否存在，不存在的话退出登录
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType(SessionManager.ACCOUNT_TYPE);

        if (accounts == null || accounts.length == 0) {
            SessionManager.getInstance().remove();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
