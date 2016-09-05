package com.linghui.fire.update;

/**
 * Created by yhzhang on 2015/12/13.
 */
public class UpdateEvent {

    public static final int ACTION_CANCEL = 0;
    public static final int ACTION_UPDATE = 1;

    public UpdateEvent(int action) {
        mAction = action;
    }

    public int getAction() {
        return mAction;
    }

    private int mAction;
}
