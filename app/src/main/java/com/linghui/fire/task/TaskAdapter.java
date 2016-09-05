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
 * Created by Yanghai on 2015/10/12.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public interface TaskListener {
        void onTaskAssigned(Task task);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView taskIcon;
        public TextView taskDescriptionText;
        public TextView taskRefundSpeedText;
        public TextView taskRefundSpeedHoursText;
        public TextView taskActualRefundPriceText;
        public TextView taskPriceText;
        public TextView taskAssignmentBtn;
        public TextView taskLeftNumText;

        public ViewHolder(View itemView) {
            super(itemView);
            taskIcon = (ImageView)itemView.findViewById(R.id.task_icon);
            taskDescriptionText = (TextView)itemView.findViewById(R.id.task_description);
            taskRefundSpeedText = (TextView)itemView.findViewById(R.id.task_refund_speed);
            taskRefundSpeedHoursText = (TextView)itemView.findViewById(R.id.task_refund_speed_hours);
            taskActualRefundPriceText = (TextView)itemView.findViewById(R.id.task_actual_offer_price);
            taskPriceText = (TextView)itemView.findViewById(R.id.task_price);
            taskAssignmentBtn = (TextView)itemView.findViewById(R.id.task_assignment_btn);
            taskLeftNumText = (TextView)itemView.findViewById(R.id.task_left_num);
        }
    }

    public TaskAdapter(Context context, TaskListener listener) {
        mContext = context;
        mTaskListener = listener;
        mTaskList = new ArrayList<Task>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.task_list_item, null);
        ViewHolder holder = new ViewHolder(view);
        holder.taskAssignmentBtn.setOnClickListener(mTaskRequestBtnClickListener);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mTaskList.get(position);
        Task.Type taskType = Task.getType(task.taskType);
        holder.taskIcon.setImageResource(taskType != null ? taskType.iconRes : R.drawable.transparent);
        holder.taskDescriptionText.setText(task.taskDesc);
        holder.taskActualRefundPriceText.setText(String.valueOf(task.actualOfferPrice));
        holder.taskPriceText.setText(mContext.getString(R.string.task_price, task.taskPrice));
        holder.taskRefundSpeedText.setText(TaskInfoUtils.getRefundSpeedText(mContext, task.refundSpeed));
        if (task.refundSpeed == 0) {
            holder.taskRefundSpeedHoursText.setVisibility(View.GONE);
        } else {
            holder.taskRefundSpeedHoursText.setText(mContext.getString(R.string.task_refund_speed_with_hours, task.refundSpeed));
            holder.taskRefundSpeedHoursText.setVisibility(View.VISIBLE);
        }
        holder.taskAssignmentBtn.setText(R.string.get_right_now);
        holder.taskAssignmentBtn.setTag(task);
        holder.taskLeftNumText.setText(mContext.getString(R.string.task_left_num, task.taskNum - task.claimedNum));
    }

    @Override
    public int getItemCount() {
        return mTaskList.size();
    }

    public void addTasks(List<Task> tasks) {
        mTaskList.addAll(tasks);
        notifyDataSetChanged();
    }

    public Task getTask(int position) {
        if (position >= 0 && position < mTaskList.size()) {
            return mTaskList.get(position);
        }

        return null;
    }

    public void clear() {
        mTaskList.clear();
        notifyDataSetChanged();
    }

    private View.OnClickListener mTaskRequestBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Task task = (Task)view.getTag();
            if (mTaskListener != null) {
                mTaskListener.onTaskAssigned(task);
            }
        }
    };

    private Context mContext;
    private TaskListener mTaskListener;
    private List<Task> mTaskList;
}
