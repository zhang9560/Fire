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
 * Created by yhzhang on 2015/11/11.
 */
public class RankingFragment extends Fragment {
    public static final String TAG = RankingFragment.class.getSimpleName();

    private class RankingAdapter extends BaseAdapter {

        private class ViewHolder {
            public TextView userName;
            public TextView todayIncome;
        }

        @Override
        public int getCount() {
            return mRankingList.size();
        }

        @Override
        public Object getItem(int position) {
            return mRankingList.get(position);
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
                holder.userName = (TextView)convertView.findViewById(R.id.user_name_text);
                holder.todayIncome = (TextView)convertView.findViewById(R.id.today_income_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            Pair<String, String> rankingItem = (Pair<String, String>)getItem(position);
            holder.userName.setText(rankingItem.first);
            holder.todayIncome.setText("¥ " + rankingItem.second);

            return convertView;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListView = new ListView(getContext());
        mListView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return mListView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.pref_title_ranking);

        VolleyUtils.getInstance().sendRequest(API.rankingRequest(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "ranking : " + response);

                if (response.optInt("status") == API.RESPONSE_STATUS_OK) {
                    JSONArray result = response.optJSONArray("result");

                    if (result != null) {
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject object = result.optJSONObject(i);
                            mRankingList.add(new Pair<String, String>(object.optString("userName"), object.optString("todayIncome")));
                        }

                        mListView.setAdapter(new RankingAdapter());
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "ranking error : " + error);
            }
        }));
    }

    private ListView mListView;
    private List<Pair<String, String>> mRankingList = new ArrayList<Pair<String, String>>();
}
