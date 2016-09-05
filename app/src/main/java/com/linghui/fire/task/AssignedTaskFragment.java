package com.linghui.fire.task;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import com.linghui.fire.session.SessionManager;
import com.squareup.otto.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Yanghai on 2015/9/29.
 */
public class AssignedTaskFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, AssignedTaskAdapter.TaskListener {
    public static final String TAG = AssignedTaskFragment.class.getSimpleName();

    private static final int REQUEST_CODE_TASK_DETAIL_INFO = 100;

    private static final int MESSAGE_REFRESH_COUNT_DOWN = 100;

    private enum FreshAction {
        FRESH_ACTION_NO_ACTION,
        FRESH_ACTION_PULL_DOWN,
        FRESH_ACTION_PULL_UP
    }

    private static class UIHandler extends Handler {
        private WeakReference<AssignedTaskFragment> mFrag;

        public UIHandler(AssignedTaskFragment frag) {
            mFrag = new WeakReference<AssignedTaskFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_REFRESH_COUNT_DOWN && mFrag.get() != null && mFrag.get().mEnableCountDown) {
                mFrag.get().mTaskAdapter.notifyDataSetChanged();
                sendEmptyMessageDelayed(MESSAGE_REFRESH_COUNT_DOWN, 1000);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_hall, null);
        rootView.addView(initTabBar(inflater), 0);
        mSwipeRefreshContainer = (SwipeRefreshLayout)rootView.findViewById(R.id.swipe_container);
        mSwipeRefreshContainer.setOnRefreshListener(this);

        mPullToRefreshText = (TextView)rootView.findViewById(R.id.pull_to_refresh_text);

        mContentList = (RecyclerView)rootView.findViewById(R.id.list);
        mTaskAdapter = new AssignedTaskAdapter(getContext(), this);
        mContentList.setAdapter(mTaskAdapter);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mContentList.setLayoutManager(layoutManager);
        mContentList.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int firstPos = layoutManager.findFirstCompletelyVisibleItemPosition();
                if (firstPos > 0 || mFreshAction == FreshAction.FRESH_ACTION_PULL_UP) {
                    mSwipeRefreshContainer.setEnabled(false);
                } else {
                    mSwipeRefreshContainer.setEnabled(true);
                }

                int lastPos = layoutManager.findLastCompletelyVisibleItemPosition();
                int count = mTaskAdapter.getItemCount();
                if (lastPos == count - 1 && newState == RecyclerView.SCROLL_STATE_IDLE && mTaskAdapter.getItemCount() > 0 && mFreshAction == FreshAction.FRESH_ACTION_NO_ACTION) {
                    // 此处用来出来滑动到底加载更多
                    mFreshAction = FreshAction.FRESH_ACTION_PULL_UP;
                    mSnackBar = Snackbar.make(getView(), R.string.load_more, Snackbar.LENGTH_INDEFINITE);
                    mSnackBar.show();

                    VolleyUtils.getInstance().sendRequest(API.assignedTaskListRequest(API.DEFAULT_TASK_LIST_PAGE_SIZE, mFinishStatus,
                            mTaskAdapter.getTask(mTaskAdapter.getItemCount() - 1).assignTime, true,
                            mAssignedTaskListResponseListener, mAssignedTaskListErrorListener));
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setMessage(getString(R.string.sending));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(true);

        // 第一次进入刷新数据
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshContainer.setEnabled(true);
                mTabBarClickListener.onClick(mNewTaskTabItem);
            }
        }, 500);

        SessionManager.getInstance().getEventBus().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        enableCountdown(mFinishStatus == 0);

        if (mTaskStatusChanged) {
            mTaskStatusChanged = false;

            if (mTaskStatus == AssignedTask.TASK_STATUS_ASSIGNED && mCurrentTabItem != mNewTaskTabItem) {
                mSwipeRefreshContainer.setEnabled(true);
                mTabBarClickListener.onClick(mNewTaskTabItem);
            } else if (mTaskStatus == AssignedTask.TASK_STATUS_COMPLETED && mCurrentTabItem != mCompletedTabItem) {
                mSwipeRefreshContainer.setEnabled(true);
                mTabBarClickListener.onClick(mCompletedTabItem);
            } else {
                mSwipeRefreshContainer.setRefreshing(true);
                onRefresh();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        enableCountdown(false);
    }

    @Override
    public void onDestroyView () {
        if (mSwipeRefreshContainer.isRefreshing()) {
            mSwipeRefreshContainer.setRefreshing(false);
            mSwipeRefreshContainer.destroyDrawingCache();
            mSwipeRefreshContainer.clearAnimation();
        }

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SessionManager.getInstance().getEventBus().unregister(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_DETAIL_INFO && resultCode == Activity.RESULT_OK) {
            if (mFinishStatus == 0) {
                AssignedTask task = mTaskAdapter.getTask(0);
                if (task.status == AssignedTask.TASK_STATUS_ASSIGNED) {
                    task.status = AssignedTask.TASK_STATUS_DOING;
                    mTaskAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onRefresh() {
        mFreshAction = FreshAction.FRESH_ACTION_PULL_DOWN;
        VolleyUtils.getInstance().sendRequest(API.assignedTaskListRequest(API.DEFAULT_TASK_LIST_PAGE_SIZE, mFinishStatus, null, false,
                mAssignedTaskListResponseListener, mAssignedTaskListErrorListener));
    }

    @Override
    public void onItemClicked(String assignId) {
        Intent intent = new Intent(getContext(), TaskDetailInfoActivity.class);
        intent.putExtra(TaskDetailInfoActivity.EXTRA_KEY_ASSIGN_ID, assignId);
        startActivityForResult(intent, REQUEST_CODE_TASK_DETAIL_INFO);
    }

    @Override
    public void onItemLongClicked(final String assignId) {
        new AlertDialog.Builder(getContext()).setItems(R.array.assigned_task_context_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // 删除任务
                        new AlertDialog.Builder(getContext()).setTitle(R.string.stop_playing)
                                .setMessage(R.string.deletion_tip).setNegativeButton(R.string.regretted, null)
                                .setPositiveButton(R.string.still_delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mProgressDialog.show();
                                        VolleyUtils.getInstance().sendRequest(API.deleteAssignedTaskRequest(assignId, mDeleteAssignedTaskResponseListener, mDeleteAssignedTaskErrorListener));
                                    }
                                }).show();
                        break;
                }
            }
        }).show();
    }

    @Override
    public void onConfirmButtonClicked(String assignId) {
        mProgressDialog.show();
        VolleyUtils.getInstance().sendRequest(API.confirmAssignedTaskRequest(assignId, mConfirmAssignedTaskResponseListener, mConfirmAssignedTaskErrorListener));
    }

    @Subscribe
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        mTaskStatusChanged = true;
        mTaskStatus = event.getStatus();
    }

    private ViewGroup initTabBar(LayoutInflater inflater) {
        ViewGroup tabBar = (ViewGroup)inflater.inflate(R.layout.task_tab_bar, null);
        ViewGroup tabItems = (ViewGroup)tabBar.getChildAt(0);
        for (int i = 0; i < tabItems.getChildCount(); i++) {
            tabItems.getChildAt(i).setOnClickListener(mTabBarClickListener);
        }

        mNewTaskTabItem = tabItems.getChildAt(0);
        mCompletedTabItem = tabItems.getChildAt(1);
        TextView newTaskTabText = (TextView)((ViewGroup) mNewTaskTabItem).getChildAt(0);
        newTaskTabText.setTextColor(getResources().getColor(R.color.colorPrimary));
        newTaskTabText.setBackgroundResource(R.drawable.shape_blue_under_line);

        return tabBar;
    }

    private void enableCountdown(boolean enabled) {
        mEnableCountDown = enabled;

        if (enabled) {
            mTaskAdapter.notifyDataSetChanged();
            mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH_COUNT_DOWN, 1000);
        } else {
            mHandler.removeMessages(MESSAGE_REFRESH_COUNT_DOWN);
        }
    }

    private View.OnClickListener mTabBarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ViewGroup viewGroup = (ViewGroup)view;
            TextView tabItemText = (TextView)viewGroup.getChildAt(0);
            TextView currentTabItemText = mCurrentTabItem == null ? null : (TextView) ((ViewGroup) mCurrentTabItem).getChildAt(0);

            if (view == mCurrentTabItem || mFreshAction != FreshAction.FRESH_ACTION_NO_ACTION) return;
            mCurrentTabItem = view;

            if (currentTabItemText != null) {
                currentTabItemText.setTextColor(getResources().getColor(R.color.tab_item_text_color));
                currentTabItemText.setBackgroundResource(0);
            }

            tabItemText.setTextColor(getResources().getColor(R.color.colorPrimary));
            tabItemText.setBackgroundResource(R.drawable.shape_blue_under_line);

            switch (tabItemText.getId()) {
                case R.id.tab_new:
                    mFinishStatus = 0;
                    break;
                case R.id.tab_completed:
                    mFinishStatus = 1;
                    break;
            }

            enableCountdown(mFinishStatus == 0);
            mSwipeRefreshContainer.setRefreshing(true);
            onRefresh();
        }
    };

    private Response.Listener<JSONObject> mAssignedTaskListResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "assignedTaskList : " + response);
            if (mSwipeRefreshContainer.isRefreshing()) mSwipeRefreshContainer.setRefreshing(false);
            if (mSnackBar != null && mSnackBar.isShown())mSnackBar.dismiss();

            if (mFreshAction == FreshAction.FRESH_ACTION_PULL_DOWN) {
                mTaskAdapter.clear();
            }

            if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                JSONObject result = response.optJSONObject("result");
                if (result != null) {
                    JSONArray dataHolder = result.optJSONArray("dataHolder");
                    if (dataHolder != null) {
                        List<AssignedTask> taskList = new ArrayList<AssignedTask>();

                        for (int i = 0; i < dataHolder.length(); i++) {
                            AssignedTask task = AssignedTask.fromJsonObject(dataHolder.optJSONObject(i));

                            if (task != null) {
                                taskList.add(task);
                            }
                        }

                        mTaskAdapter.addTasks(taskList);

                        if (mFreshAction == FreshAction.FRESH_ACTION_PULL_UP && taskList.size() > 1) {
                            mContentList.smoothScrollToPosition(mTaskAdapter.getItemCount() - taskList.size());
                        }
                    }
                }
            }

            if (mTaskAdapter.getItemCount() > 0) {
                mPullToRefreshText.setVisibility(View.GONE);
            } else {
                mPullToRefreshText.setVisibility(View.VISIBLE);
            }

            mFreshAction = FreshAction.FRESH_ACTION_NO_ACTION;
        }
    };

    private Response.ErrorListener mAssignedTaskListErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "assignedTaskList error : " + error);
            if (mSwipeRefreshContainer.isRefreshing()) mSwipeRefreshContainer.setRefreshing(false);
            if (mSnackBar != null && mSnackBar.isShown()) mSnackBar.dismiss();

            if (error instanceof NoConnectionError) {
                Snackbar.make(getView(), R.string.no_connection, Snackbar.LENGTH_LONG).show();
            }

            mFreshAction = FreshAction.FRESH_ACTION_NO_ACTION;
        }
    };

    private Response.Listener<JSONObject> mDeleteAssignedTaskResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(final JSONObject response) {
            Log.d(TAG, "deleteAssignedTask : " + response);
            mProgressDialog.dismiss();

            Snackbar.make(getView(), response.optString("message"), Snackbar.LENGTH_SHORT).show();
            if (response.optInt("status") == API.RESPONSE_STATUS_DELETED) {
                mSwipeRefreshContainer.setRefreshing(true);
                onRefresh();
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

    private Response.Listener<JSONObject> mConfirmAssignedTaskResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(final JSONObject response) {
            Log.d(TAG, "confirmAssignedTask : " + response);
            mProgressDialog.dismiss();

            Snackbar.make(getView(), response.optString("message"), Snackbar.LENGTH_SHORT).show();
            if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
                mSwipeRefreshContainer.setRefreshing(true);
                onRefresh();
            }
        }
    };

    private Response.ErrorListener mConfirmAssignedTaskErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "confirmAssignedTask error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private boolean mEnableCountDown = false;
    private UIHandler mHandler = new UIHandler(this);

    private SwipeRefreshLayout mSwipeRefreshContainer;
    private RecyclerView mContentList;
    private AssignedTaskAdapter mTaskAdapter;
    private TextView mPullToRefreshText;

    private int mFinishStatus;
    private Snackbar mSnackBar;
    private FreshAction mFreshAction = FreshAction.FRESH_ACTION_NO_ACTION;
    private boolean mTaskStatusChanged = false;
    private int mTaskStatus;

    private ProgressDialog mProgressDialog;

    private View mNewTaskTabItem = null;
    private View mCompletedTabItem = null;
    private View mCurrentTabItem = null;
}
