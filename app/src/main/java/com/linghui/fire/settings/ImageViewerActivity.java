package com.linghui.fire.settings;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.facebook.drawee.view.SimpleDraweeView;
import com.linghui.fire.R;
import com.linghui.fire.widget.SessionBaseActivity;

/**
 * Created by yhzhang on 2015/11/22.
 */
public class ImageViewerActivity extends SessionBaseActivity {
    public static final String EXTRA_KEY_IMAGE_URI = "image_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        SimpleDraweeView image = (SimpleDraweeView)findViewById(R.id.image);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        String imageUri = getIntent().getStringExtra(EXTRA_KEY_IMAGE_URI);
        image.setImageURI(Uri.parse(imageUri));
    }
}
