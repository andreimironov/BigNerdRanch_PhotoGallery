package com.andreimironov.andrei.photogallery;

import android.app.Application;
import android.os.StrictMode;

public class PhotoGalleryApplication extends Application {
    private boolean DEVELOPER_MODE = true;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }
}
