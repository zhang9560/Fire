package com.linghui.fire.widget;

import android.app.Dialog;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import com.linghui.fire.R;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

/**
 * Created by yhzhang on 2015/12/16.
 */
public class ShareFragment extends DialogFragment implements View.OnClickListener {
    public static final String TAG = ShareFragment.class.getSimpleName();

    private static final String APP_ID = "wx5d90e7eb015a1045";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWXAPI = WXAPIFactory.createWXAPI(getContext(), APP_ID, true);
        mWXAPI.registerApp(APP_ID);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_share, null);
        mWeChatFriendsView = view.findViewById(R.id.wechat_friends);
        mWeChatFriendsView.setOnClickListener(this);
        mWeChatMomentsView = view.findViewById(R.id.wechat_moments);
        mWeChatMomentsView.setOnClickListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.share_to)
                .setView(view)
                .setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onClick(View view) {
        if (view == mWeChatFriendsView) {
            sendWXMessage(SendMessageToWX.Req.WXSceneSession);
        } else if (view == mWeChatMomentsView) {
            sendWXMessage(SendMessageToWX.Req.WXSceneTimeline);
        }

        dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWXAPI.unregisterApp();
    }

    private void sendWXMessage(int scene) {
        WXWebpageObject webpageObject = new WXWebpageObject();
        webpageObject.webpageUrl="http://www.hu-xiu.com/down.html";

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = webpageObject;
        msg.setThumbImage(((BitmapDrawable)getContext().getResources().getDrawable(R.drawable.ic_launcher)).getBitmap());

        switch (scene) {
            case SendMessageToWX.Req.WXSceneSession:
                msg.title = getString(R.string.app_name);
                msg.description = getString(R.string.app_description);
                break;
            case SendMessageToWX.Req.WXSceneTimeline:
                msg.title = getString(R.string.app_description);
                break;
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        req.scene = scene;

        mWXAPI.sendReq(req);
    }

    private View mWeChatFriendsView;
    private View mWeChatMomentsView;

    private IWXAPI mWXAPI;
}
