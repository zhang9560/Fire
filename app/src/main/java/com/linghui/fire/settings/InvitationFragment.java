package com.linghui.fire.settings;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.linghui.fire.R;
import com.linghui.fire.server.API;
import com.linghui.fire.server.VolleyUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yhzhang on 2015/11/11.
 */
public class InvitationFragment extends Fragment implements ListView.OnScrollListener {
    public static final String TAG = InvitationFragment.class.getSimpleName();

    private static class InvitationItem {
        public String userName;
        public String overidingAmount;
        public String assignmentCount;
    }

    private class InvitationAdapter extends BaseAdapter {

        private class ViewHolder {
            public TextView userNameText;
            public TextView overidingAmountText;
        }

        @Override
        public int getCount() {
            return mInvitationList.size();
        }

        @Override
        public Object getItem(int position) {
            return mInvitationList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int postion, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.ranking_list_item, null);
                holder = new ViewHolder();
                holder.userNameText = (TextView)convertView.findViewById(R.id.user_name_text);
                holder.overidingAmountText = (TextView)convertView.findViewById(R.id.today_income_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            InvitationItem item = (InvitationItem)getItem(postion);
            holder.userNameText.setText(item.userName);
            holder.overidingAmountText.setText("¥ " + item.overidingAmount);

            return convertView;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListView = new ListView(getContext());
        mListView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mListView.setOnScrollListener(this);
        mAdapter = new InvitationAdapter();
        mListView.setAdapter(mAdapter);

        return mListView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_invitation);

        fetchInvitationList();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                // 判断滚动到底部
                if (view.getLastVisiblePosition() == (view.getCount() - 1) && !mIsFetchingList) {
                    mSnackbar = Snackbar.make(getView(), R.string.load_more, Snackbar.LENGTH_INDEFINITE);
                    mSnackbar.show();
                    fetchInvitationList();
                }
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    private void fetchInvitationList() {
        mIsFetchingList = true;
        VolleyUtils.getInstance().sendRequest(API.invitationRecordsRequest(mPage, API.DEFAULT_TASK_LIST_PAGE_SIZE, mListener, mErrorListener));
    }

    private Response.Listener<JSONObject> mListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            Log.d(TAG, "invitationRecords : " + response);
            if (mSnackbar != null && mSnackbar.isShown()) mSnackbar.dismiss();

            if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                JSONObject result = response.optJSONObject("result");

                if (result != null) {
                    JSONArray array = result.optJSONArray("dataHolder");

                    if (array != null && array.length() > 0) {
                        mPage++;

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.optJSONObject(i);
                            InvitationItem item = new InvitationItem();
                            item.userName = obj.optString("subUserName");
                            item.overidingAmount = obj.optString("subOveridingAmount");
                            item.assignmentCount = obj.optString("subAssignmentCount");
                            mInvitationList.add(item);
                        }

                        mAdapter.notifyDataSetChanged();
                    }
                }
            }

            mIsFetchingList = false;
        }
    };

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "invitationRecords error : " + error);
            if (mSnackbar != null && mSnackbar.isShown()) mSnackbar.dismiss();
            mIsFetchingList = false;
        }
    };

    private int mPage = 0;
    private boolean mIsFetchingList = false;

    private Snackbar mSnackbar;

    private ListView mListView;
    private InvitationAdapter mAdapter;
    private List<InvitationItem> mInvitationList = new ArrayList<InvitationItem>();
}
