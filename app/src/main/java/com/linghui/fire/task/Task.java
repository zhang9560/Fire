package com.linghui.fire.task;

import com.linghui.fire.R;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Yanghai on 2015/10/7.
 */
public class Task {
    public static final int TASK_CATEGORY_COMMISSION = 1;
    public static final int TASK_CATEGORY_TRIAL = 2;
    public static final int TASK_CATEGORY_TRAFFIC = 3;
    public static final int TASK_CATEGORY_OTHERS = 9;

    public static class Type {
        public int id;
        public int iconRes;
        public int nameRes;

        public Type(int id, int iconRes, int nameRes) {
            this.id = id;
            this.iconRes = iconRes;
            this.nameRes = nameRes;
        }
    }

    private static Type[] sTypes = {
            new Type(1, R.drawable.ic_taobao, R.string.task_type_taobao_commission),
            new Type(2, R.drawable.ic_tianmao, R.string.task_type_tianmao_commission),
            new Type(5, R.drawable.ic_traffic, R.string.task_type_taobao_traffic),
            new Type(6, R.drawable.ic_traffic, R.string.task_type_tianmao_traffic)
    };

    public static Type getType(int taskTypeId) {
        for (Type type : sTypes) {
            if (type.id == taskTypeId) {
                return type;
            }
        }

        return null;
    }

    public String taskIcon;
    public String taskId;
    public String taskDesc;
    public int taskNum;
    public String leftNum;
    public double actualOfferPrice;
    public double productPrice;
    public int refundSpeed;
    public String targetUrl;
    public String targetItemId;
    public double guaranteeRate;
    public ArrayList<Tag> randomTags;
    public int taskCategory;
    public int taskType;
    public String taskStatus;
    public int claimedNum;
    public String offerPrice;
    public double taskPrice;
    public String mainImg;
    public int quantity;
    public String taskDetail;
    public String effectiveDate;
    public String expireDate;
    public String organId;
    public String creatorId;
    public String organName;
    public String taskAddress;
    public String clientType;
    public String reqContent;
    public String reqId;
    public double releaseCost;
    public String taskSteps;
    public int grabCost;
    public String ownerId;

    public static Task fromJsonObject(JSONObject obj) {
        if (obj != null) {
            Task task = new Task();
            task.taskIcon = obj.optString("taskIcon");
            task.taskId = obj.optString("taskId");
            task.taskDesc = obj.optString("taskDesc");
            task.taskNum = obj.optInt("taskNum");
            task.leftNum = obj.optString("leftNum");
            task.actualOfferPrice = obj.optDouble("actualOfferPrice");
            task.productPrice = obj.optDouble("productPrice");
            task.refundSpeed = obj.optInt("refundSpeed");
            task.targetUrl = obj.optString("targetUrl");
            task.targetItemId = obj.optString("targetItemId");
            task.guaranteeRate = obj.optDouble("guaranteeRate");
            JSONArray tags = obj.optJSONArray("randomTags");
            if (tags != null) {
                task.randomTags = new ArrayList<Tag>();
                for (int i = 0; i < tags.length(); i++) {
                    task.randomTags.add(Tag.fromJsonObject(tags.optJSONObject(i)));
                }
            }
            task.taskCategory = obj.optInt("taskCategory");
            task.taskType = obj.optInt("taskType");
            task.taskStatus = obj.optString("taskStatus");
            task.claimedNum = obj.optInt("claimedNum");
            task.offerPrice = obj.optString("offerPrice");
            task.taskPrice = obj.optDouble("taskPrice");
            task.mainImg = obj.optString("mainImg");
            task.quantity = obj.optInt("quantity");
            task.taskDetail = obj.optString("taskDetail");
            task.effectiveDate = obj.optString("effectiveDate");
            task.expireDate = obj.optString("expireDate");
            task.organId = obj.optString("organId");
            task.creatorId = obj.optString("creatorId");
            task.organName = obj.optString("organName");
            task.taskAddress = obj.optString("taskAddress");
            task.clientType = obj.optString("clientType");
            task.reqContent = obj.optString("reqContent");
            task.reqId = obj.optString("reqId");
            task.releaseCost = obj.optDouble("releaseCost");
            task.taskSteps = obj.optString("taskSteps");
            task.grabCost = obj.optInt("grabCost");
            task.ownerId = obj.optString("ownerId");

            return task;
        }

        return null;
    }
}
