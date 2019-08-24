package com.shinow.qrscan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public final class CheckPermissionUtils {

    public static void initPermission(Activity activity) {
        String[] permissions = CheckPermissionUtils.checkPermission(activity);
        if (permissions.length != 0) {
            ActivityCompat.requestPermissions(activity, permissions, 100);
        }
    }

    private static String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private CheckPermissionUtils() {
    }

    private static String[] checkPermission(Context context) {
        List<String> data = new ArrayList<>();
        for (String permission : permissions) {
            int checkSelfPermission = ContextCompat.checkSelfPermission(context, permission);
            if (checkSelfPermission == PackageManager.PERMISSION_DENIED) {
                data.add(permission);
            }
        }
        return data.toArray(new String[0]);
    }

}
