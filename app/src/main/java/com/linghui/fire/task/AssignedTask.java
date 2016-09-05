package com.linghui.fire.task;

import org.json.JSONObject;

/**
 * Created by yhzhang on 2015/11/23.
 */
public class AssignedTask {
    public static final int TASK_STATUS_ASSIGNED = 0;
    public static final int TASK_STATUS_DOING = 1;
    public static final int TASK_STATUS_SUBMITTED = 2;
    public static final int TASK_STATUS_CONFIRMING = 3;
    public static final int TASK_STATUS_COMPLETED = 4;
    public static final int TASK_STATUS_CANCELLED = 5;

    public String assignId;
    public String taskDesc;
    public int taskType;
    public int status;
    public String expireDate;
    public String assignTime;
    public String submitTime;
    public String refundTime;
    public double actualOfferPrice;
    public double taskPrice;

    public static AssignedTask fromJsonObject(JSONObject obj) {
        if (obj != null) {
            AssignedTask assignedTask = new AssignedTask();
            assignedTask.assignId = obj.optString("assignId");
            assignedTask.taskDesc = obj.optString("taskDesc");
            assignedTask.taskType = obj.optInt("taskType");
            assignedTask.status = obj.optInt("status");
            assignedTask.expireDate = obj.optString("expireDate");
            assignedTask.assignTime = obj.optString("assignTime");
            assignedTask.submitTime = obj.optString("submitTime");
            assignedTask.refundTime = obj.optString("refundTime");
            assignedTask.actualOfferPrice = obj.optDouble("actualOfferPrice");
            assignedTask.taskPrice = obj.optDouble("taskPrice");

            return assignedTask;
        }

        return null;
    }
}
