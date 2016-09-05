package com.linghui.fire.task;

import org.json.JSONObject;

/**
 * Created by yhzhang on 2015/11/20.
 */
public class Tag {
    public static final int TAG_TYPE_CHAT = 1;
    public static final int TAG_TYPE_ADD_TO_FAVORITES = 2;
    public static final int TAG_TYPE_ADD_TO_CART = 4;
    public static final int TAG_TYPE_VIEW_RATES = 66;

    public String name;
    public int id;

    public static Tag fromJsonObject(JSONObject obj) {
        if (obj != null) {
            Tag tag = new Tag();
            tag.name = obj.optString("tagName");
            tag.id = obj.optInt("tagId");

            if (tag.id == TAG_TYPE_CHAT ||
                tag.id == TAG_TYPE_ADD_TO_FAVORITES ||
                tag.id == TAG_TYPE_ADD_TO_CART ||
                tag.id == TAG_TYPE_VIEW_RATES) {
                return tag;
            }
        }

        return null;
    }
}
