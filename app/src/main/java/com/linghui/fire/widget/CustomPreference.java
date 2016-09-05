package com.linghui.fire.widget;

import android.content.Context;
import android.net.Uri;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.linghui.fire.R;

/**
 * Created by Yanghai on 2015/9/30.
 */
public class CustomPreference extends Preference {

    public CustomPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder viewHolder) {
        super.onBindViewHolder(viewHolder);

        SimpleDraweeView imageView = (SimpleDraweeView)viewHolder.findViewById(R.id.image);
        if (mImageUri != null) {
            int width = (int)getContext().getResources().getDimension(R.dimen.pref_default_avatar_size);
            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(mImageUri)
                    .setResizeOptions(new ResizeOptions(width, width))
                    .build();
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(request)
                    .setOldController(imageView.getController())
                    .build();
            imageView.setController(controller);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        TextView textView = (TextView)viewHolder.findViewById(R.id.text);
        if (!TextUtils.isEmpty(mText)) {
            textView.setText(mText);
            textView.setVisibility(View.VISIBLE);
        } else if (mSpannedText != null) {
            textView.setText(mSpannedText);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }

        ImageView nextImage = (ImageView)viewHolder.findViewById(R.id.next_image);
        if (mShowNextImage) {
            nextImage.setVisibility(View.VISIBLE);
        } else {
            nextImage.setVisibility(View.GONE);
        }
    }

    public void setImage(Uri imageUri) {
        mImageUri = imageUri;
        notifyChanged();
    }

    public void setText(String text) {
        mText = text;
        mSpannedText = null;
        notifyChanged();
    }

    public void setText(Spanned text) {
        mText = null;
        mSpannedText = text;
        notifyChanged();
    }

    public void setShowNextImage(boolean show) {
        mShowNextImage = show;
        notifyChanged();
    }

    public Uri mImageUri;
    public String mText;
    public Spanned mSpannedText;
    public boolean mShowNextImage = true;
}
