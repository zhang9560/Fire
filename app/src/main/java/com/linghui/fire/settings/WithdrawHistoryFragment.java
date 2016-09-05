package com.linghui.fire.settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Created by yhzhang on 2015/11/28.
 */
public class WithdrawHistoryFragment extends Fragment {
    public static final String TAG = WithdrawHistoryFragment.class.getSimpleName();

    private class WithdrawHistoryAdapter extends BaseAdapter {

        private class ViewHolder {
            public TextView applyTime;
            public TextView applyAmount;
        }

        @Override
        public int getCount() {
            return mWithdrawHistoryList.size();
        }

        @Override
        public Object getItem(int position) {
            return mWithdrawHistoryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.ranking_list_item, null);
                holder = new ViewHolder();
                holder.applyTime = (TextView)convertView.findViewById(R.id.user_name_text);
                holder.applyAmount = (TextView)convertView.findViewById(R.id.today_income_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            Pair<String, String> rankingItem = (Pair<String, String>)getItem(position);
            holder.applyTime.setText(rankingItem.first);
            holder.applyAmount.setText("¥ " + rankingItem.second);

            return convertView;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_withdraw_history, null);
        mListView = (ListView)rootView.findViewById(R.id.list);
        mWithdrawHistoryAdapter = new WithdrawHistoryAdapter();
        mListView.setAdapter(mWithdrawHistoryAdapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_withdraw_history);

        VolleyUtils.getInstance().sendRequest(API.withdrawHistoryRequest(API.DEFAULT_TASK_LIST_PAGE_SIZE, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "withdrawHistory : " + response);

                if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                    JSONObject result = response.optJSONObject("result");
                    if (result != null) {
                        JSONArray dataHolder = result.optJSONArray("dataHolder");
                        if (dataHolder != null && dataHolder.length() > 0) {

                            for (int i = 0; i < dataHolder.length(); i++) {
                                JSONObject item = dataHolder.optJSONObject(i);
                                mWithdrawHistoryList.add(new Pair<String, String>(item.optString("applyTime"), item.optString("applyAmount")));
                            }

                            mWithdrawHistoryAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "withdrawHistory error : " + error);
            }
        }));
    }

    private ListView mListView;
    private WithdrawHistoryAdapter mWithdrawHistoryAdapter;
    private List<Pair<String, String>> mWithdrawHistoryList = new ArrayList<Pair<String, String>>();
}
