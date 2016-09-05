package com.linghui.fire;

import android.support.multidex.MultiDexApplication;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.linghui.fire.session.SessionManager;

/**
 * Created by Yanghai on 2015/10/2.
 */
public class AppContext extends MultiDexApplication {

    public static AppContext getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        SessionManager.getInstance().init(this);
        initFresco();
    }

    private void initFresco() {
        DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder()
                .setBaseDirectoryPath(getExternalCacheDir())
                .setBaseDirectoryName("fresco")
                .setMaxCacheSize(100 * ByteConstants.MB)
                .build();
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setMainDiskCacheConfig(diskCacheConfig).build();

        Fresco.initialize(this, config);
    }

    private static AppContext sInstance;

    public boolean launched = false;
}
