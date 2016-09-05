package com.linghui.fire.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Yanghai on 2015/10/12.
 */
public class IconFontTextView extends TextView {

    public IconFontTextView(Context context) {
        super(context);
        initView();
    }

    public IconFontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public IconFontTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        Typeface iconFont = Typeface.createFromAsset(getContext().getAssets(), "iconfont.ttf");
        setTypeface(iconFont);
    }
}
