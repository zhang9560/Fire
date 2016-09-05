package com.linghui.fire.task;

/**
 * Created by yhzhang on 2015/12/8.
 */
public class TaskStatusChangedEvent {

    public TaskStatusChangedEvent(int status) {
        mStatus = status;
    }

    public int getStatus() {
        return mStatus;
    }

    private int mStatus;
}
