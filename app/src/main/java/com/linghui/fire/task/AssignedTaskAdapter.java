package com.linghui.fire.task;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.linghui.fire.R;
import com.linghui.fire.utils.TaskInfoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yanghai on 2015/11/23.
 */
public class AssignedTaskAdapter extends RecyclerView.Adapter<AssignedTaskAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    public interface TaskListener {
        void onItemClicked(String assignId);
        void onItemLongClicked(String assignId);
        void onConfirmButtonClicked(String assignId);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View taskItemView;
        public ImageView taskIcon;
        public TextView taskDescriptionText;
        public TextView taskRefundSpeedText;
        public TextView taskRefundSpeedHoursText;
        public TextView taskActualRefundPriceText;
        public TextView taskPriceText;
        public TextView taskStatusText;
        public TextView taskCountdownText;

        public ViewHolder(View itemView) {
            super(itemView);
            taskItemView = itemView;
            taskIcon = (ImageView)itemView.findViewById(R.id.task_icon);
            taskDescriptionText = (TextView)itemView.findViewById(R.id.task_description);
            taskRefundSpeedText = (TextView)itemView.findViewById(R.id.task_refund_speed);
            taskRefundSpeedHoursText = (TextView)itemView.findViewById(R.id.task_refund_speed_hours);
            taskActualRefundPriceText = (TextView)itemView.findViewById(R.id.task_actual_offer_price);
            taskPriceText = (TextView)itemView.findViewById(R.id.task_price);
            taskStatusText = (TextView)itemView.findViewById(R.id.task_assignment_btn);
            taskCountdownText = (TextView)itemView.findViewById(R.id.task_left_num);
        }
    }

    public AssignedTaskAdapter(Context context, TaskListener listener) {
        mContext = context;
        mAssignedTaskList = new ArrayList<AssignedTask>();
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.task_list_item, null);
        view.setClickable(true);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        ViewHolder holder = new ViewHolder(view);
        holder.taskStatusText.setOnClickListener(mConfirmButtonOnClickListener);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AssignedTask task = mAssignedTaskList.get(position);
        holder.taskItemView.setTag(task.assignId);
        Task.Type taskType = Task.getType(task.taskType);
        holder.taskIcon.setImageResource(taskType != null ? taskType.iconRes : R.drawable.transparent);
        holder.taskDescriptionText.setText(task.taskDesc);
        holder.taskActualRefundPriceText.setText(String.valueOf(task.actualOfferPrice));
        holder.taskPriceText.setText(mContext.getString(R.string.task_price, task.taskPrice));

        holder.taskStatusText.setTag(task.assignId);
        holder.taskStatusText.setVisibility(View.VISIBLE);
        holder.taskStatusText.setEnabled(false);
        holder.taskCountdownText.setVisibility(View.VISIBLE);
        switch (task.status) {
            case AssignedTask.TASK_STATUS_ASSIGNED:
                holder.taskStatusText.setText(R.string.assigned_task_status_assigned);
                holder.taskCountdownText.setText(TaskInfoUtils.getCountdownString(mContext, R.string.countdown_prefix_order, task.assignTime, task.expireDate, TaskInfoUtils.ONE_HOUR).toString());
                break;
            case AssignedTask.TASK_STATUS_DOING:
                holder.taskStatusText.setText(R.string.assigned_task_status_doing);
                holder.taskCountdownText.setText(TaskInfoUtils.getCountdownString(mContext, R.string.countdown_prefix_order, task.assignTime, task.expireDate, TaskInfoUtils.ONE_HOUR).toString());
                break;
            case AssignedTask.TASK_STATUS_SUBMITTED:
                holder.taskStatusText.setText(R.string.assigned_task_status_submitted);
                holder.taskCountdownText.setText(TaskInfoUtils.getCountdownString(mContext, R.string.countdown_prefix_refund, task.submitTime, task.expireDate, TaskInfoUtils.ONE_DAY).toString());
                break;
            case AssignedTask.TASK_STATUS_CONFIRMING:
                holder.taskStatusText.setText(R.string.assigned_task_status_confirmed);
                holder.taskCountdownText.setText(TaskInfoUtils.getCountdownString(mContext, R.string.countdown_prefix_confirm, task.refundTime, task.expireDate, TaskInfoUtils.HALF_DAY).toString());
                holder.taskStatusText.setEnabled(true);
                break;
            case AssignedTask.TASK_STATUS_COMPLETED:
                holder.taskStatusText.setVisibility(View.GONE);
                holder.taskStatusText.setText(R.string.assigned_task_status_completed);
                holder.taskCountdownText.setVisibility(View.GONE);
                break;
            case AssignedTask.TASK_STATUS_CANCELLED:
                holder.taskStatusText.setVisibility(View.GONE);
                holder.taskStatusText.setText(R.string.assigned_task_status_cancelled);
                holder.taskCountdownText.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mAssignedTaskList.size();
    }

    @Override
    public void onClick(View view) {
        if (mListener != null) {
            mListener.onItemClicked((String)view.getTag());
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (mListener != null) {
            mListener.onItemLongClicked((String)view.getTag());
            return true;
        }
        return false;
    }

    public void addTasks(List<AssignedTask> tasks) {
        mAssignedTaskList.addAll(tasks);
        notifyDataSetChanged();
    }

    public AssignedTask getTask(int position) {
        if (position >= 0 && position < mAssignedTaskList.size()) {
            return mAssignedTaskList.get(position);
        }

        return null;
    }

    public void clear() {
        mAssignedTaskList.clear();
        notifyDataSetChanged();
    }

    private View.OnClickListener mConfirmButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mListener != null) {
                mListener.onConfirmButtonClicked((String)view.getTag());
            }
        }
    };

    private Context mContext;
    private List<AssignedTask> mAssignedTaskList;
    private TaskListener mListener;
}
