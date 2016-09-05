package com.linghui.fire.task;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.settings.GeneralActivity;
import com.linghui.fire.settings.RankingFragment;
import com.linghui.fire.settings.RealNameAuthFragment;
import com.linghui.fire.settings.TaobaoAccountBindingFragment;
import com.linghui.fire.settings.TenpayFragment;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Yanghai on 2015/9/28.
 */
public class HallFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, TaskAdapter.TaskListener {
    public static final String TAG = HallFragment.class.getSimpleName();

    private enum FreshAction {
        FRESH_ACTION_NO_ACTION,
        FRESH_ACTION_PULL_DOWN,
        FRESH_ACTION_PULL_UP
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_hall, null);
        rootView.addView(initTabBar(inflater), 0);

        mAdvertisementView = (ViewFlipper)rootView.findViewById(R.id.advertisements);
        mSwipeRefreshContainer = (SwipeRefreshLayout)rootView.findViewById(R.id.swipe_container);
        mSwipeRefreshContainer.setEnabled(false);
        mSwipeRefreshContainer.setOnRefreshListener(this);

        mContentList = (RecyclerView)rootView.findViewById(R.id.list);
        mTaskAdapter = new TaskAdapter(getContext(), this);
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

                    VolleyUtils.getInstance().sendRequest(API.hallTaskListRequest(API.DEFAULT_TASK_LIST_PAGE_SIZE,
                            mTaskAdapter.getTask(mTaskAdapter.getItemCount() - 1).effectiveDate, true, mTaskCategory, mHallTaskListResponseListener, mHallTaskListErrorListener));
                }
            }
        });

        mPullToRefreshText = (TextView)rootView.findViewById(R.id.pull_to_refresh_text);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage(getContext().getString(R.string.sending));

        // 第一次进入刷新数据
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshContainer.setEnabled(true);
                mTabBarClickListener.onClick(mInitTabItem);
                getAdvertisements();
            }
        }, 500);
    }

    @Override
    public void onDestroyView () {
        if (mSwipeRefreshContainer.isRefreshing()) {
            mSwipeRefreshContainer.setRefreshing(false);
            mSwipeRefreshContainer.destroyDrawingCache();
            mSwipeRefreshContainer.clearAnimation();
        }

        mAdvertisementView.stopFlipping();
        super.onDestroyView();
    }

    @Override
    public void onRefresh() {
        mFreshAction = FreshAction.FRESH_ACTION_PULL_DOWN;
        VolleyUtils.getInstance().sendRequest(API.hallTaskListRequest(API.DEFAULT_TASK_LIST_PAGE_SIZE, null, false, mTaskCategory, mHallTaskListResponseListener, mHallTaskListErrorListener));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ranking, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ranking:
                Intent intent = new Intent(getContext(), GeneralActivity.class);
                intent.setAction(RankingFragment.class.getName());
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTaskAssigned(Task task) {
        mProgressDialog.show();
        VolleyUtils.getInstance().sendRequest(API.assignTaskRequest(task.taskId, createTags(task),
                mAssignTaskResponseListener, mAssignTaskErrorListener));
    }

    private ViewGroup initTabBar(LayoutInflater inflater) {
        ViewGroup tabBar = (ViewGroup)inflater.inflate(R.layout.hall_tab_bar, null);
        ViewGroup tabItems = (ViewGroup)tabBar.getChildAt(0);
        for (int i = 0; i < tabItems.getChildCount(); i++) {
            tabItems.getChildAt(i).setOnClickListener(mTabBarClickListener);
        }

        mInitTabItem = tabItems.getChildAt(0);
        TextView initTabText = (TextView)((ViewGroup)mInitTabItem).getChildAt(0);
        initTabText.setTextColor(getResources().getColor(R.color.colorPrimary));
        initTabText.setBackgroundResource(R.drawable.shape_blue_under_line);

        return tabBar;
    }

    private void getAdvertisements() {
        VolleyUtils.getInstance().sendRequest(API.hallAdvertisementsRequest(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "hallAdvertisements response : " + response);
                if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                    JSONArray adArray = response.optJSONArray("result");
                    if (adArray != null && adArray.length() > 0) {
                        mAdvertisementView.setVisibility(View.VISIBLE);

                        for (int i = 0; i < adArray.length(); i++) {
                            JSONObject adObj = adArray.optJSONObject(i);
                            if (adObj != null) {
                                String adverImg = adObj.optString("adverImg");
                                if (adverImg != null) {
                                    SimpleDraweeView draweeView = new SimpleDraweeView(getContext());
                                    draweeView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                    draweeView.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
                                    draweeView.setTag(adObj.optString("adverUrl"));
                                    draweeView.setClickable(true);
                                    draweeView.setOnClickListener(onAdvertisementClickListener);
                                    draweeView.setImageURI(Uri.parse(adverImg));
                                    mAdvertisementView.addView(draweeView);
                                }
                            }
                        }
                        mAdvertisementView.startFlipping();
                    } else {
                        mAdvertisementView.setVisibility(View.GONE);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "hallAdvertisements error : " + error);
            }
        }));
    }

    private String createTags(Task task) {
        boolean hasChatTag;
        boolean hasAddToFavoritesTag;
        boolean hasAddToCartTag;
        boolean hasViewRatesTag;
        Random random = new Random();

        switch (task.taskCategory) {
            case Task.TASK_CATEGORY_TRAFFIC:
                hasChatTag = random.nextInt(4) == 0;
                hasAddToFavoritesTag = random.nextInt(4) == 0;
                hasAddToCartTag = random.nextInt(4) == 0;
                hasViewRatesTag = random.nextInt(4) == 0;
                break;
            default:
                hasChatTag = random.nextBoolean();
                hasAddToFavoritesTag = random.nextBoolean();
                hasAddToCartTag = random.nextBoolean();
                hasViewRatesTag = random.nextBoolean();
                break;
        }

        StringBuilder sb = new StringBuilder();
        if (hasChatTag) sb.append(Tag.TAG_TYPE_CHAT).append(",");
        if (hasAddToFavoritesTag) sb.append(Tag.TAG_TYPE_ADD_TO_FAVORITES).append(",");
        if (hasAddToCartTag) sb.append(Tag.TAG_TYPE_ADD_TO_CART).append(",");
        if (hasViewRatesTag) sb.append(Tag.TAG_TYPE_VIEW_RATES).append(",");

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
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
                case R.id.tab_all:
                    mTaskCategory = null;
                    break;
                case R.id.tab_commission:
                    mTaskCategory = Task.TASK_CATEGORY_COMMISSION;
                    break;
                case R.id.tab_traffic:
                    mTaskCategory = Task.TASK_CATEGORY_TRAFFIC;
                    break;
                case R.id.tab_others:
                    mTaskCategory = Task.TASK_CATEGORY_OTHERS;
                    break;
            }

            mSwipeRefreshContainer.setRefreshing(true);
            onRefresh();
        }
    };

    private View.OnClickListener onAdvertisementClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String url = (String)view.getTag();
            Intent intent = new Intent(getContext(), GeneralActivity.class);
            intent.setAction(AdverFragment.class.getName());
            intent.putExtra(AdverFragment.EXTRA_KEY_URL, url);
            startActivity(intent);
        }
    };

    private Response.ErrorListener mHallTaskListErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "hallTaskList error : " + error);
            if (mSwipeRefreshContainer.isRefreshing()) mSwipeRefreshContainer.setRefreshing(false);
            if (mSnackBar != null && mSnackBar.isShown()) mSnackBar.dismiss();

            if (error instanceof NoConnectionError) {
                Snackbar.make(getView(), R.string.no_connection, Snackbar.LENGTH_LONG).show();
            }

            mFreshAction = FreshAction.FRESH_ACTION_NO_ACTION;
        }
    };

    private Response.Listener<JSONObject> mHallTaskListResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "hallTaskList : " + response);
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
                        List<Task> taskList = new ArrayList<Task>();

                        for (int i = 0; i < dataHolder.length(); i++) {
                            Task task = Task.fromJsonObject(dataHolder.optJSONObject(i));

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

    private Response.ErrorListener mAssignTaskErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "assignTask error : " + error);
            mProgressDialog.dismiss();
        }
    };

    private Response.Listener<JSONObject> mAssignTaskResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "assignTask : " + response);
            mProgressDialog.dismiss();

            if (response.optInt("status") == API.RESPONSE_STATUS_CREATED) {
                Snackbar.make(getView(), R.string.assign_task_success, Snackbar.LENGTH_SHORT).show();
                SessionManager.getInstance().getEventBus().post(new TaskAssignedEvent());
                SessionManager.getInstance().getEventBus().post(new TaskStatusChangedEvent(AssignedTask.TASK_STATUS_ASSIGNED));
                mSwipeRefreshContainer.setRefreshing(true);
                onRefresh();
            } else {
                JSONObject result = response.optJSONObject("result");
                if (result != null) {
                    int errorCode = result.optInt("errorCode");
                    String errorMessage = result.optString("errorMessage");
                    if (errorCode > 0 && !TextUtils.isEmpty(errorMessage)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                                .setTitle(R.string.binding_failed_title)
                                .setMessage(errorMessage);

                        switch (errorCode) {
                            case API.TASK_ASSIGNMENT_ERROR_CODE_NO_REAL_NAME_AUTH:
                                builder.setPositiveButton(R.string.do_real_name_auth, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(getContext(), GeneralActivity.class);
                                                intent.setAction(RealNameAuthFragment.class.getName());
                                                startActivity(intent);
                                            }
                                        }).show();
                                break;
                            case API.TASK_ASSIGNMENT_ERROR_CODE_NOT_BIND_TENPAY:
                                builder.setPositiveButton(R.string.binding_immediately, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(getContext(), GeneralActivity.class);
                                        intent.setAction(TenpayFragment.class.getName());
                                        startActivity(intent);
                                    }
                                }).show();
                                break;
                            case API.TASK_ASSIGNMENT_ERROR_CODE_NOT_BIND_TAOBAO:
                                builder.setPositiveButton(R.string.binding_immediately, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(getContext(), GeneralActivity.class);
                                        intent.setAction(TaobaoAccountBindingFragment.class.getName());
                                        startActivity(intent);
                                    }
                                }).show();
                                break;
                            case API.TASK_ASSIGNMENT_ERROR_CODE_NOT_PASS_TEST:
                                builder.setPositiveButton(R.string.do_exam, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(getContext(), GeneralActivity.class);
                                        intent.setAction(ExamFragment.class.getName());
                                        startActivity(intent);
                                    }
                                }).show();
                                break;
                            default:
                                builder.setPositiveButton(R.string.binding_failed_ok, null).show();
                                break;
                        }
                    }
                }
            }
        }
    };

    private Handler mHandler = new Handler();

    private ViewFlipper mAdvertisementView;
    private SwipeRefreshLayout mSwipeRefreshContainer;
    private RecyclerView mContentList;
    private TaskAdapter mTaskAdapter;
    private TextView mPullToRefreshText;
    private Snackbar mSnackBar;
    private ProgressDialog mProgressDialog;

    private FreshAction mFreshAction = FreshAction.FRESH_ACTION_NO_ACTION;
    private Integer mTaskCategory;

    private View mInitTabItem = null;
    private View mCurrentTabItem = null;
}
