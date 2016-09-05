package com.linghui.fire.settings;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.utils.TaskInfoUtils;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Yanghai on 2015/10/12.
 */
public class RealNameAuthFragment extends Fragment {
    public static final String TAG = RealNameAuthFragment.class.getSimpleName();

    private static final int REQUEST_CODE_CAPTURE_PICTURE = 100;
    public static final int REQUEST_CODE_PICK_PICTURE = 101;

    private static final int MESSAGE_UPLOAD_PICTURE_FINISHED = 0;
    private static final int MESSAGE_UPLOAD_PICTURE_RESULT_FAILED = 0;
    private static final int MESSAGE_UPLOAD_PICTURE_RESULT_SUCCESS = 1;

    private static final int PERMISSION_REQUEST_CODE_CAPTURE_PICTURE = 100;
    private static final int PERMISSION_REQUEST_CODE_PICK_PICTURE = 101;

    private static class UIHandler extends Handler {
        private WeakReference<RealNameAuthFragment> mFrag;

        public UIHandler(RealNameAuthFragment frag) {
            mFrag = new WeakReference<RealNameAuthFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_UPLOAD_PICTURE_FINISHED && mFrag.get() != null) {
                RealNameAuthFragment frag = mFrag.get();

                frag.mUploadedPicturesCount++;

                if (msg.arg1 == MESSAGE_UPLOAD_PICTURE_RESULT_FAILED) {
                    frag.mUploadedPicturesFailedCount++;
                }

                JSONObject attach = new JSONObject();
                try {
                    attach.put("attachType", msg.arg2);
                    attach.put("attachPath", msg.obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                frag.mAttachArray.put(attach);

                // 全部上传完毕
                if (frag.mUploadedPicturesCount == 4) {
                    // 有图片上传失败
                    if (frag.mUploadedPicturesFailedCount > 0) {
                        frag.mProgressDialog.dismiss();
                        new AlertDialog.Builder(frag.getContext()).setMessage(R.string.real_name_auth_upload_failed).setPositiveButton(R.string.confirm, null).show();
                    } else {
                        // 全部图片上传成功后提交实名认证
                        VolleyUtils.getInstance().sendRequest(API.realNameAuthRequest(frag.mIdCardEdit.getText().toString(),
                                frag.mAttachArray.toString(), frag.mRealNameAuthResponseListener, frag.mRealNameAuthErrorListener));
                    }

                    frag.mUploadedPicturesCount = 0;
                    frag.mUploadedPicturesFailedCount = 0;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealNameAuthStatus = SessionManager.getInstance().getRealNameAuthStatus();
        setHasOptionsMenu(canUploadPictures());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_real_name_auth, null);

        TextView contentText = (TextView)rootView.findViewById(R.id.real_name_auth_content);
        switch (mRealNameAuthStatus) {
            case SessionManager.REAL_NAME_AUTH_STATUS_IN_REVIEW:
                contentText.setText(R.string.real_name_auth_tip3);
                break;
            case SessionManager.REAL_NAME_AUTH_STATUS_INDENTIFIED:
                contentText.setText(R.string.real_name_auth_tip4);
                break;
            default:
                contentText.setText(R.string.real_name_auth_tip1);
                break;
        }

        ((TextView)rootView.findViewById(R.id.real_name_auth_tip)).setText(Html.fromHtml(getString(R.string.real_name_auth_tip2)));
        mIdCardEdit = (EditText)rootView.findViewById(R.id.id_card);
        mIdCardEdit.setEnabled(canUploadPictures());

        mTitleArray = new TextView[] {
                (TextView)rootView.findViewById(R.id.title_1),
                (TextView)rootView.findViewById(R.id.title_2),
                (TextView)rootView.findViewById(R.id.title_3),
                (TextView)rootView.findViewById(R.id.title_4)
        };

        mPictureArray = new SimpleDraweeView[] {
                (SimpleDraweeView)rootView.findViewById(R.id.picture_1),
                (SimpleDraweeView)rootView.findViewById(R.id.picture_2),
                (SimpleDraweeView)rootView.findViewById(R.id.picture_3),
                (SimpleDraweeView)rootView.findViewById(R.id.picture_4)
        };
        for (SimpleDraweeView pictureView : mPictureArray) {
            pictureView.setOnClickListener(mPictureOnClickListener);
        }

        mDeleteBtnArray = new ImageView[] {
                (ImageView)rootView.findViewById(R.id.delete_btn_1),
                (ImageView)rootView.findViewById(R.id.delete_btn_2),
                (ImageView)rootView.findViewById(R.id.delete_btn_3),
                (ImageView)rootView.findViewById(R.id.delete_btn_4)
        };
        for (ImageView deleteBtn : mDeleteBtnArray) {
            deleteBtn.setOnClickListener(mDeleteBtnOnClickListener);
            deleteBtn.setVisibility(mRealNameAuthStatus == SessionManager.REAL_NAME_AUTH_STATUS_REJECTED ? View.VISIBLE : View.GONE);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_real_name_auth);

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage(getString(R.string.getting_real_name_auth_info));
        mProgressDialog.show();

        VolleyUtils.getInstance().sendRequest(API.realNameAuthInfoRequest(mRealNameAuthInfoResponseListener, mRealNameAuthInfoErrorListener));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_PICTURE && resultCode == Activity.RESULT_OK) {
            String[] proj = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContext().getContentResolver().query(data.getData(), proj, null, null, null);
            if (cursor.moveToFirst()) {
                Uri pictureFileUri = Uri.fromFile(new File(cursor.getString(0)));
                mCurrentClickedImageView.setTag(pictureFileUri);
                mCurrentClickedImageView.setController(createDraweeController(mCurrentClickedImageView, pictureFileUri));
                getDeleteBtnByPictureView(mCurrentClickedImageView).setVisibility(View.VISIBLE);
            }
            cursor.close();
        } else if (requestCode == REQUEST_CODE_CAPTURE_PICTURE && resultCode == Activity.RESULT_OK) {
            mCurrentClickedImageView.setTag(mPictureUri);
            mCurrentClickedImageView.setController(createDraweeController(mCurrentClickedImageView, mPictureUri));
            getDeleteBtnByPictureView(mCurrentClickedImageView).setVisibility(View.VISIBLE);

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(mPictureUri);
            getContext().sendBroadcast(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_CAPTURE_PICTURE:
                boolean hasAllPermissions = true;
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        hasAllPermissions = false;
                        break;
                    }
                }

                if (hasAllPermissions) {
                    capturePicture();
                }
                break;
            case PERMISSION_REQUEST_CODE_PICK_PICTURE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickPicture();
                }
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.real_name_auth, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.submit:
                doRealNameAuth();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ImageView getDeleteBtnByPictureView(SimpleDraweeView pictureView) {
        for (int i = 0; i < mPictureArray.length; i++) {
            if (pictureView == mPictureArray[i]) {
                return mDeleteBtnArray[i];
            }
        }

        return null;
    }

    private SimpleDraweeView getPictureViewByDeleteBtn(ImageView deleteBtn) {
        for (int i = 0; i < mDeleteBtnArray.length; i++) {
            if (deleteBtn == mDeleteBtnArray[i]) {
                return mPictureArray[i];
            }
        }

        return null;
    }

    private void doRealNameAuth() {
        if (!TextUtils.isEmpty(mIdCardEdit.getText()) && allPicturesSet()) {
            mProgressDialog.setMessage(getString(R.string.real_name_auth_uploading));
            mProgressDialog.show();
            VolleyUtils.getInstance().sendRequest(API.getStorageUploadTokenRequest(mGetStorageUploadTokenResponseListener, mGetStorageUploadTokenErrorListener));
        }
    }

    private boolean allPicturesSet() {
        for (SimpleDraweeView pictureView : mPictureArray) {
            Object pictureUriObj = pictureView.getTag();
            if (pictureUriObj == null) {
                return false;
            }
        }
        return true;
    }

    private void uploadPictures(String pictureUri, final int attachType, String uploadToken) {
        String fileScheme = "file://";
        if (mUploadManager == null) {
            mUploadManager = new UploadManager();
        }

        mUploadManager.put(pictureUri.substring(fileScheme.length()), null, uploadToken, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                Message message = new Message();
                message.what = MESSAGE_UPLOAD_PICTURE_FINISHED;
                message.arg1 = info.isOK() ? MESSAGE_UPLOAD_PICTURE_RESULT_SUCCESS : MESSAGE_UPLOAD_PICTURE_RESULT_FAILED;
                message.arg2 = attachType;
                message.obj = response.optString("key");
                mHandler.sendMessage(message);
            }
        }, null);
    }

    private boolean canUploadPictures() {
        return mRealNameAuthStatus == SessionManager.REAL_NAME_AUTH_STATUS_NOT_IDENTIFIED ||
                mRealNameAuthStatus == SessionManager.REAL_NAME_AUTH_STATUS_REJECTED;
    }

    private void capturePicture() {
        File pictureDir = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
        if (!pictureDir.exists()) {
            pictureDir.mkdirs();
        }

        File pictureFile = new File(pictureDir, System.currentTimeMillis() + ".jpg");
        mPictureUri = Uri.fromFile(pictureFile);
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);
        startActivityForResult(intent, REQUEST_CODE_CAPTURE_PICTURE);
    }

    private void pickPicture() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_PICTURE);
    }

    private Response.Listener<JSONObject> mRealNameAuthInfoResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "realNameAuthInfo : " + response);
            mProgressDialog.dismiss();

            if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                JSONArray result = response.optJSONArray("result");
                if (result != null) {
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject attach = result.optJSONObject(i);
                        int attachType = attach.optInt("attachType");
                        String attachTitle = attach.optString("attachTitle");
                        String attachPath = attach.optString("attachPath");
                        switch (attachType) {
                            case 1:
                                mTitleArray[0].setText(attachTitle);
                                mAttachTypeArray[0] = attachType;
                                if (!TaskInfoUtils.isEmptyValue(attachPath)) {
                                    mPictureArray[0].setController(createDraweeController(mPictureArray[0], Uri.parse(API.getPictureUrlFromQiNiu(attachPath))));
                                }
                                break;
                            case 2:
                                mTitleArray[1].setText(attachTitle);
                                mAttachTypeArray[1] = attachType;
                                if (!TaskInfoUtils.isEmptyValue(attachPath)) {
                                    mPictureArray[1].setController(createDraweeController(mPictureArray[1], Uri.parse(API.getPictureUrlFromQiNiu(attachPath))));
                                }
                                break;
                            case 3:
                                mTitleArray[2].setText(attachTitle);
                                mAttachTypeArray[2] = attachType;
                                if (!TaskInfoUtils.isEmptyValue(attachPath)) {
                                    mPictureArray[2].setController(createDraweeController(mPictureArray[2], Uri.parse(API.getPictureUrlFromQiNiu(attachPath))));
                                }
                                break;
                            default:
                                mTitleArray[3].setText(attachTitle);
                                mAttachTypeArray[3] = attachType;
                                if (!TaskInfoUtils.isEmptyValue(attachPath)) {
                                    mPictureArray[3].setController(createDraweeController(mPictureArray[3], Uri.parse(API.getPictureUrlFromQiNiu(attachPath))));
                                }
                                break;
                        }
                    }
                }
            }
        }
    };

    private DraweeController createDraweeController(SimpleDraweeView draweeView, Uri uri) {
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(new ResizeOptions((int)getResources().getDimension(R.dimen.real_name_auth_photo_width),
                        (int)getResources().getDimension(R.dimen.real_name_auth_photo_height)))
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(request)
                .setOldController(draweeView.getController())
                .build();

        return controller;
    }

    private Response.ErrorListener mRealNameAuthInfoErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "realNameAuthInfo error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private Response.Listener<JSONObject> mRealNameAuthResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "realNameAuth : " + response);
            mProgressDialog.dismiss();

            if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
                SessionManager.getInstance().syncUserInfo();
                getActivity().finish();
            } else {
                new AlertDialog.Builder(getContext()).setMessage(response.optString("message")).setPositiveButton(R.string.confirm, null).show();
            }
        }
    };

    private Response.ErrorListener mRealNameAuthErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "realNameAuth error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private View.OnClickListener mPictureOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mCurrentClickedImageView = (SimpleDraweeView) view;

            Object pictureUrlObj = view.getTag();
            if (pictureUrlObj == null) {
                if (canUploadPictures()) {
                    new AlertDialog.Builder(getContext()).setItems(R.array.choose_avatar, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            switch (which) {
                                case 0:
                                    boolean hasStoragePermission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                                    boolean hasCameraPermission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                                    if (hasStoragePermission && hasCameraPermission) {
                                        capturePicture();
                                    } else {
                                        ArrayList<String> permissionList = new ArrayList<String>();
                                        if (!hasStoragePermission) permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                        if (!hasCameraPermission) permissionList.add(Manifest.permission.CAMERA);

                                        String[] permissionArray = new String[permissionList.size()];
                                        requestPermissions(permissionList.toArray(permissionArray), PERMISSION_REQUEST_CODE_CAPTURE_PICTURE);
                                    }

                                    break;
                                case 1:
                                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                        pickPicture();
                                    } else {
                                        requestPermissions(new String[] {
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_PICK_PICTURE);
                                    }
                                    break;
                            }
                        }
                    }).show();
                }
            } else {
                Intent intent = new Intent(getContext(), ImageViewerActivity.class);
                intent.putExtra(ImageViewerActivity.EXTRA_KEY_IMAGE_URI, pictureUrlObj.toString());
                startActivity(intent);
            }
        }
    };

    private Response.Listener<JSONObject> mGetStorageUploadTokenResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "getStorageUploadToken : " + response);

            if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                JSONObject result = response.optJSONObject("result");
                String token = result.optString("tokenValue");

                for (int i = 0; i < mPictureArray.length; i++) {
                    uploadPictures(mPictureArray[i].getTag().toString(), mAttachTypeArray[i], token);
                }
            } else {
                mProgressDialog.dismiss();
                new AlertDialog.Builder(getContext()).setTitle(R.string.pref_title_real_name_auth)
                        .setMessage(response.optString("message")).setPositiveButton(R.string.confirm, null).show();
            }
        }
    };

    private Response.ErrorListener mGetStorageUploadTokenErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "getStorageUploadToken error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private View.OnClickListener mDeleteBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SimpleDraweeView pictureView = getPictureViewByDeleteBtn((ImageView)view);

            if (pictureView != null) {
                view.setVisibility(View.GONE);
                pictureView.setTag(null);
                pictureView.setController(createDraweeController(pictureView, Uri.parse("res:///" + R.drawable.transparent)));
            }
        }
    };

    private UIHandler mHandler = new UIHandler(this);

    private ProgressDialog mProgressDialog;
    private EditText mIdCardEdit;
    private TextView[] mTitleArray;
    private SimpleDraweeView[] mPictureArray;
    private ImageView[] mDeleteBtnArray;
    private SimpleDraweeView mCurrentClickedImageView;
    private int[] mAttachTypeArray = new int[4];
    private JSONArray mAttachArray = new JSONArray();
    private Uri mPictureUri;

    private UploadManager mUploadManager;
    private int mUploadedPicturesCount = 0;
    private int mUploadedPicturesFailedCount = 0;

    private int mRealNameAuthStatus = SessionManager.REAL_NAME_AUTH_STATUS_NOT_IDENTIFIED;
}
