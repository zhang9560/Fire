package com.linghui.fire.update;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.aspsine.multithreaddownload.CallBack;
import com.aspsine.multithreaddownload.DownloadConfiguration;
import com.aspsine.multithreaddownload.DownloadException;
import com.aspsine.multithreaddownload.DownloadManager;
import com.aspsine.multithreaddownload.DownloadRequest;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.utils.TaskInfoUtils;
import com.squareup.otto.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;


/**
 * Created by yhzhang on 2015/12/12.
 */
public class UpdateService extends Service implements Response.Listener<JSONObject>, Response.ErrorListener {
    public static final String TAG = UpdateService.class.getSimpleName();

    public static final String ACTION_CHECK_LATEST_VERSION = "check_latest_version";

    private static final String UPDATE_FILE_NAME = "com_linghui_fire.apk";

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        SessionManager.getInstance().getEventBus().register(this);

        DownloadConfiguration configuration = new DownloadConfiguration();
        configuration.setMaxThreadNum(1);
        configuration.setThreadNum(1);
        DownloadManager.getInstance().init(this, configuration);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (!TextUtils.isEmpty(action)) {
            if (action.equals(ACTION_CHECK_LATEST_VERSION) && !mIsWorking) {
                mIsWorking = true;
                VolleyUtils.getInstance().sendRequest(
                        API.latestClientVersionRequest(UpdateService.this, UpdateService.this));
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SessionManager.getInstance().getEventBus().unregister(this);
    }

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        switch (event.getAction()) {
            case UpdateEvent.ACTION_CANCEL:
                mIsWorking = false;
                break;
            case UpdateEvent.ACTION_UPDATE:
                download(mUpdatePackageUrl);
                break;
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "latestClientVersion error : " + error);

        mIsWorking = false;
    }

    @Override
    public void onResponse(JSONObject response) {
        Log.d(TAG, "latestClientVersion : " + response);

        if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
            JSONObject result = response.optJSONObject("result");

            if (result != null) {
                int versionCode = result.optInt("infoValue");

                try {
                    int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

                    if (versionCode > currentVersionCode) {
                        VolleyUtils.getInstance().sendRequest(API.releasedVersionsRequest(versionCode - 1, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d(TAG, "releasedVersions : " + response);

                                if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                                    JSONArray array = response.optJSONArray("result");

                                    if (array != null && array.length() > 0) {
                                        JSONObject updateInfo = array.optJSONObject(0);
                                        mUpdatePackageUrl = updateInfo.optString("patchPath");

                                        Intent intent = new Intent(UpdateService.this, UpdateActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    } else {
                                        mIsWorking = false;
                                    }
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(TAG, "releasedVersions error : " + error);

                                mIsWorking = false;
                            }
                        }));
                    } else {
                        mIsWorking = false;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    mIsWorking = false;
                }
            } else {
                mIsWorking = false;
            }
        } else {
            mIsWorking = false;
        }
    }

    private void download(String url) {
        if (!TaskInfoUtils.isEmptyValue(url)) {
            File cacheDir = getExternalCacheDir();

            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            if (cacheDir.exists()) {
                DownloadRequest request = new DownloadRequest.Builder()
                        .setTitle(UPDATE_FILE_NAME)
                        .setUri(url)
                        .setFolder(cacheDir).build();
                DownloadManager.getInstance().download(request, "update", mDownloadCallback);
            } else {
                mIsWorking = false;
            }
        } else {
            mIsWorking = false;
        }
    }

    private CallBack mDownloadCallback = new CallBack() {
        int mLastProgress = -1;

        @Override
        public void onStarted() {
            Log.i(TAG, "[onStarted]");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(UpdateService.this)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.preparing_new_version));
            mNotificationManager.notify(0, builder.build());

        }

        @Override
        public void onConnecting() {
            Log.i(TAG, "[onConnecting]");
        }

        @Override
        public void onConnected(long total, boolean isRangeSupport) {
            Log.i(TAG, "[onConnected]");
        }

        @Override
        public void onProgress(long finished, long total, int progress) {
            Log.i(TAG, "[onProgress] progress = " + progress);

            if (progress != mLastProgress) {
                mLastProgress = progress;
                NotificationCompat.Builder builder = new NotificationCompat.Builder(UpdateService.this)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.downloading_new_version, progress))
                        .setOngoing(true)
                        .setProgress(100, progress, false);

                mNotificationManager.notify(0, builder.build());
            }
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "[onCompleted]");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(UpdateService.this)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.download_completed))
                    .setAutoCancel(true);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(getExternalCacheDir().getAbsolutePath() + "/" + UPDATE_FILE_NAME)),
                    "application/vnd.android.package-archive");
            PendingIntent contentIntent = PendingIntent.getActivity(UpdateService.this, 0, intent, 0);

            builder.setContentIntent(contentIntent);
            mNotificationManager.notify(0, builder.build());

            startActivity(intent);
            mIsWorking = false;
        }

        @Override
        public void onDownloadPaused() {
            Log.i(TAG, "[onDownloadPaused]");
        }

        @Override
        public void onDownloadCanceled() {
            Log.i(TAG, "[onDownloadCanceled]");
            mLastProgress = -1;
            mIsWorking = false;
        }

        @Override
        public void onFailed(DownloadException e) {
            Log.i(TAG, "[onFailed]");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(UpdateService.this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.download_failed))
                    .setAutoCancel(true);
            mNotificationManager.notify(0, builder.build());

            mLastProgress = -1;
            mIsWorking = false;
        }
    };

    private boolean mIsWorking = false;
    private String mUpdatePackageUrl;

    private NotificationManager mNotificationManager;
}
