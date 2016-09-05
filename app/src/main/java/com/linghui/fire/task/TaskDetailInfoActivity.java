package com.linghui.fire.task;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.drawee.view.SimpleDraweeView;
import com.linghui.fire.R;
import com.linghui.fire.message.CustomerServiceActivity;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.utils.TaskInfoUtils;
import com.linghui.fire.widget.SessionBaseActivity;
import com.linghui.fire.widget.ShareFragment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Created by yhzhang on 2015/11/23.
 */
public class TaskDetailInfoActivity extends SessionBaseActivity {
    public static final String TAG = TaskDetailInfoActivity.class.getSimpleName();

    public static final String EXTRA_KEY_ASSIGN_ID = "assign_id";

    private static final int MESSAGE_REFRESH_COUNT_DOWN = 100;

    private static final int REQUEST_CODE_START_TASK = 100;

    private static class UIHandler extends Handler {
        private WeakReference<TaskDetailInfoActivity> mActivity;

        public UIHandler(TaskDetailInfoActivity activity) {
            mActivity = new WeakReference<TaskDetailInfoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_REFRESH_COUNT_DOWN && mActivity.get() != null && mActivity.get().mEnableCountDown) {
                mActivity.get().refreshTaskCountdown();
                sendEmptyMessageDelayed(MESSAGE_REFRESH_COUNT_DOWN, 1000);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail_info);

        initViews();

        mAssignId = getIntent().getStringExtra(EXTRA_KEY_ASSIGN_ID);
        if (!TextUtils.isEmpty(mAssignId)) {
            mProgressDialog.show();
            VolleyUtils.getInstance().sendRequest(API.taskDetailInfoRequest(mAssignId, mTaskDetailInfoResponseListener, mTaskDetailInfoErrorListener));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableCountdown(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        enableCountdown(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_detail_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.share:
                ShareFragment shareFragment = new ShareFragment();
                shareFragment.show(getSupportFragmentManager(), "share");
                return true;
            case R.id.delete:
                new AlertDialog.Builder(this).setTitle(R.string.stop_playing)
                        .setMessage(R.string.deletion_tip).setNegativeButton(R.string.regretted, null)
                        .setPositiveButton(R.string.still_delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mProgressDialog.setMessage(getString(R.string.sending));
                                mProgressDialog.show();
                                VolleyUtils.getInstance().sendRequest(API.deleteAssignedTaskRequest(mAssignId, mDeleteAssignedTaskResponseListener, mDeleteAssignedTaskErrorListener));
                            }
                        }).show();
                return true;
            case R.id.complaint:
                Intent intent = new Intent(this, CustomerServiceActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_START_TASK && resultCode == RESULT_OK) {
            finish();
        }
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.fetching_task_detail_info));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);

        mMainImageView = (SimpleDraweeView)findViewById(R.id.main_image);
        mActualOfferPriceText = (TextView)findViewById(R.id.task_actual_offer_price);
        mTaskPriceText = (TextView)findViewById(R.id.task_price);
        mTaskBtn = (TextView)findViewById(R.id.task_btn);
        mTaskCountdownText = (TextView)findViewById(R.id.task_countdown);
        mOrganNameText = (TextView)findViewById(R.id.organ_name);
        mRefundSpeedText = (TextView)findViewById(R.id.refund_speed);
        mRefundSpeedHoursText = (TextView)findViewById(R.id.refund_speed_hours);
        mTaskIdText = (TextView)findViewById(R.id.task_id);
        mTaskCategoryText = (TextView)findViewById(R.id.task_category);
        mTaskOrganNameText = (TextView)findViewById(R.id.task_organ_name);
        mTaskThirdPartyNameText = (TextView)findViewById(R.id.task_third_party_name);
        mTaskTagsLayout = findViewById(R.id.task_tags_layout);
        mTagArea = (ViewGroup)findViewById(R.id.tag_area);
    }

    public void onTaskBtnClicked(View view) {
        if (!TextUtils.isEmpty(mTaskUrl)) {
            Intent intent = new Intent(TaskDetailInfoActivity.this, TaskViewerActivity.class);
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_ASSIGN_ID, mAssignId);
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_URL, mTaskUrl);
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_QUANTITY, mTaskQuantity);
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_PRICE, mTaskPrice);
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_TAGS, mTaskTags.toString());
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_STATUS, mStatus);
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_CATEGORY, mTaskCategory);
            intent.putExtra(TaskViewerActivity.EXTRA_KEY_TASK_TARGET_ITEM_ID, mTaskTargetId);
            startActivityForResult(intent, REQUEST_CODE_START_TASK);
        }
    }

    public void onViewCourseBtnClicked(View view) {
        Intent intent = new Intent(this, CourseViewActivity.class);
        startActivity(intent);
    }

    public void onOwnerMobileClicked(View view) {
        if (!TextUtils.isEmpty(mOwnerMobile)) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mOwnerMobile));
            startActivity(intent);
        }
    }

    private void addTags(JSONArray tagArray) {
        if (tagArray != null) {
            for (int i = 0; i < tagArray.length(); i++) {
                Tag tag = Tag.fromJsonObject(tagArray.optJSONObject(i));

                if (tag != null) {
                    TextView tagView = (TextView) LayoutInflater.from(this).inflate(R.layout.tag_item, null);
                    tagView.setTag(tag);
                    tagView.setText(tag.name);
                    tagView.setOnClickListener(mOnTagViewClicked);
                    mTagArea.addView(tagView);
                }
            }

            if (mTagArea.getChildCount() > 0) {
                mTaskTagsLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void enableCountdown(boolean enabled) {
        mEnableCountDown = enabled;

        if (enabled) {
            refreshTaskCountdown();
            mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH_COUNT_DOWN, 1000);
        } else {
            mHandler.removeMessages(MESSAGE_REFRESH_COUNT_DOWN);
        }
    }

    private void refreshTaskCountdown() {
        TaskInfoUtils.CountdownString countdownString = null;

        switch (mStatus) {
            case AssignedTask.TASK_STATUS_ASSIGNED:
            case AssignedTask.TASK_STATUS_DOING:
                countdownString = TaskInfoUtils.getCountdownString(this, R.string.countdown_prefix_order, mAssignTime, mExpireTime, TaskInfoUtils.ONE_HOUR);
                mTaskCountdownText.setText(countdownString.toString());
                break;
            case AssignedTask.TASK_STATUS_SUBMITTED:
                countdownString = TaskInfoUtils.getCountdownString(this, R.string.countdown_prefix_refund, mSubmitTime, mExpireTime, TaskInfoUtils.ONE_DAY);
                mTaskCountdownText.setText(countdownString.toString());
                break;
            case AssignedTask.TASK_STATUS_CONFIRMING:
                countdownString = TaskInfoUtils.getCountdownString(this, R.string.countdown_prefix_confirm, mRefundTime, mExpireTime, TaskInfoUtils.HALF_DAY);
                mTaskCountdownText.setText(countdownString.toString());
                break;
            case AssignedTask.TASK_STATUS_COMPLETED:
                mTaskCountdownText.setVisibility(View.GONE);
                enableCountdown(false);
                break;
            case AssignedTask.TASK_STATUS_CANCELLED:
                mTaskCountdownText.setVisibility(View.GONE);
                enableCountdown(false);
            default:
                mTaskCountdownText.setText("");
                break;
        }

        if (countdownString != null && countdownString.getLeftTime() == 0) {
            enableCountdown(false);
            mTaskBtn.setText(R.string.view_task);
        }
    }

    private Response.Listener<JSONObject> mTaskDetailInfoResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "taskDetailInfo : " + response);
            mProgressDialog.dismiss();

            if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                setResult(RESULT_OK);

                JSONObject result = response.optJSONObject("result");
                JSONObject taskInfoVo = result.optJSONObject("taskInfoVo");
                if (taskInfoVo != null) {
                    mMainImageView.setImageURI(Uri.parse(API.getPictureUrlFromQiNiu(taskInfoVo.optString("mainImg"))));
                    mActualOfferPriceText.setText(String.format("%.1f", result.opt("actualOfferPrice")));
                    mOrganNameText.setText(result.optString("organName"));
                    mTaskIdText.setText(getString(R.string.task_id, result.optString("taskId")));
                    mTaskOrganNameText.setText(getString(R.string.task_organ_name, result.optString("organName")));
                    mTaskThirdPartyNameText.setText(getString(R.string.task_third_party_name, result.optString("thirdPartyName")));

                    int taskType = result.optInt("taskType");
                    Task.Type taskTypeObj = Task.getType(taskType);
                    mTaskCategoryText.setText(getString(R.string.task_type_prefix,
                            taskTypeObj != null ? getString(taskTypeObj.nameRes) : getString(R.string.task_type_unknown)));

                    mTaskUrl = taskInfoVo.optString("taskAddress");
                    mTaskCategory = taskInfoVo.optInt("taskCategory");
                    mTaskTargetId = taskInfoVo.optString("targetItemId");
                    mTaskQuantity = result.optInt("quantity");
                    mTaskPrice = result.optDouble("taskPrice");
                    mTaskTags = result.optJSONArray("randomTags");
                    mOwnerMobile = result.optString("ownerMobile");
                    mStatus = result.optInt("status");
                    mAssignTime = result.optString("assignTime");
                    mExpireTime = result.optString("expireDate");
                    mSubmitTime = result.optString("submitTime");
                    mRefundTime = result.optString("refundTime");

                    if (mStatus == AssignedTask.TASK_STATUS_ASSIGNED || mStatus == AssignedTask.TASK_STATUS_DOING) {
                        mTaskBtn.setText(R.string.start_task);
                    } else {
                        mTaskBtn.setText(R.string.view_task);
                    }
                    mTaskPriceText.setText(Html.fromHtml(String.format(getString(R.string.task_price_format_string), mTaskQuantity, mTaskPrice)));
                    addTags(mTaskTags);
                    refreshTaskCountdown();
                }
            } else {
                new AlertDialog.Builder(TaskDetailInfoActivity.this).setMessage(response.optString("message"))
                        .setPositiveButton(R.string.confirm, null).show();
            }
        }
    };

    private Response.ErrorListener mTaskDetailInfoErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "taskDetailInfo error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private Response.Listener<JSONObject> mDeleteAssignedTaskResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(final JSONObject response) {
            Log.d(TAG, "deleteAssignedTask : " + response);
            mProgressDialog.dismiss();

            if (response.optInt("status") == API.RESPONSE_STATUS_DELETED) {
                SessionManager.getInstance().getEventBus().post(new TaskStatusChangedEvent(AssignedTask.TASK_STATUS_CANCELLED));
                finish();
            }
        }
    };

    private Response.ErrorListener mDeleteAssignedTaskErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "deleteAssignedTask error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private View.OnClickListener mOnTagViewClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            onViewCourseBtnClicked(view);
        }
    };

    private boolean mEnableCountDown = false;
    private UIHandler mHandler = new UIHandler(this);

    private ProgressDialog mProgressDialog;
    private SimpleDraweeView mMainImageView;
    private TextView mActualOfferPriceText;
    private TextView mTaskPriceText;
    private TextView mTaskBtn;
    private TextView mTaskCountdownText;
    private TextView mOrganNameText;
    private TextView mRefundSpeedText;
    private TextView mRefundSpeedHoursText;
    private TextView mTaskIdText;
    private TextView mTaskCategoryText;
    private TextView mTaskOrganNameText;
    private TextView mTaskThirdPartyNameText;
    private View mTaskTagsLayout;
    private ViewGroup mTagArea;

    private String mAssignId;
    private String mTaskUrl;
    private int mTaskQuantity;
    private double mTaskPrice;
    private JSONArray mTaskTags;
    private int mTaskCategory;
    private String mTaskTargetId;

    private String mOwnerMobile;
    private int mStatus = -1;
    private String mAssignTime;
    private String mExpireTime;
    private String mSubmitTime;
    private String mRefundTime;
}
