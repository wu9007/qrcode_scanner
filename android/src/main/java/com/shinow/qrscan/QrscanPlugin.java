package com.shinow.qrscan;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.InputStream;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

public class QrscanPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler,
        PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private static final int REQUEST_SCAN = 100;
    private static final int REQUEST_IMAGE = 101;
    private static final int REQUEST_CAMERA_PERM = 201;

    private MethodChannel channel;
    private Activity activity;
    private ActivityPluginBinding activityBinding;
    private Result pendingResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "qr_scan");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        activityBinding = binding;
        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        if (activityBinding != null) {
            activityBinding.removeActivityResultListener(this);
            activityBinding.removeRequestPermissionsResultListener(this);
            activityBinding = null;
        }
        activity = null;
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        if (activity == null) {
            result.error("NO_ACTIVITY", "Plugin is not attached to an activity", null);
            return;
        }
        switch (call.method) {
            case "scan":
                pendingResult = result;
                launchScanner();
                break;
            case "scan_photo":
                pendingResult = result;
                choosePhoto();
                break;
            case "scan_path":
                String path = call.argument("path");
                result.success(QrDecoder.decodeFile(path));
                break;
            case "scan_bytes":
                byte[] bytes = call.argument("bytes");
                result.success(QrDecoder.decodeBytes(bytes));
                break;
            case "generate_barcode":
                String code = call.argument("code");
                try {
                    result.success(QrDecoder.encodeQr(code, 400));
                } catch (Exception e) {
                    result.error("GENERATE_FAILED", e.getMessage(), null);
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void launchScanner() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERM);
            return;
        }
        Intent intent = new Intent(activity, SecondActivity.class);
        activity.startActivityForResult(intent, REQUEST_SCAN);
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(Intent.createChooser(intent, "Select image"), REQUEST_IMAGE);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                              @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA_PERM) {
            return false;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(activity, SecondActivity.class);
            activity.startActivityForResult(intent, REQUEST_SCAN);
        } else {
            finishWithError("PERMISSION_NOT_GRANTED", "Camera permission denied");
        }
        return true;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_SCAN) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                String value = intent.getStringExtra(SecondActivity.EXTRA_RESULT);
                if (value == null) {
                    String path = intent.getStringExtra(SecondActivity.EXTRA_PATH);
                    if (path != null) {
                        value = QrDecoder.decodeFile(path);
                    }
                }
                finishWithSuccess(value);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                String error = intent != null ? intent.getStringExtra("ERROR_CODE") : null;
                if (error != null) {
                    finishWithError(error, null);
                } else {
                    finishWithSuccess(null);
                }
            } else {
                finishWithSuccess(null);
            }
            return true;
        }
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK && intent != null && intent.getData() != null) {
                finishWithSuccess(decodeUri(intent.getData()));
            } else {
                finishWithSuccess(null);
            }
            return true;
        }
        return false;
    }

    private String decodeUri(Uri uri) {
        try (InputStream stream = activity.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                return null;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            try {
                return QrDecoder.decodeBitmap(bitmap);
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void finishWithSuccess(String value) {
        if (pendingResult != null) {
            pendingResult.success(value);
            pendingResult = null;
        }
    }

    private void finishWithError(String code, String message) {
        if (pendingResult != null) {
            pendingResult.error(code, message, null);
            pendingResult = null;
        }
    }
}
