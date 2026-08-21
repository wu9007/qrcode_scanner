package com.shinow.qrscan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecondActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT = "qrscan_result";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_ERROR = "ERROR_CODE";
    public static final String EXTRA_ERROR_MESSAGE = "ERROR_MESSAGE";
    public static boolean isLightOpen = false;

    private static final String TAG = "Qrscan";
    private static final int REQUEST_IMAGE = 101;
    private static final int REQUEST_CAMERA = 202;

    private PreviewView previewView;
    private ScanOverlayView overlay;
    private ImageView lightButton;
    private Camera camera;
    private Preview preview;
    private ImageAnalysis analysis;
    private ExecutorService cameraExecutor;
    private volatile boolean handled = false;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener sensorEventListener;
    private boolean cameraBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isLightOpen = false;
        setContentView(R.layout.activity_second);
        previewView = findViewById(R.id.preview_view);
        overlay = findViewById(R.id.scan_overlay);
        lightButton = findViewById(R.id.scan_light);
        cameraExecutor = Executors.newSingleThreadExecutor();

        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            sensorEventListener = new LightSensorEventListener(lightButton);
        }

        findViewById(R.id.scan_back).setOnClickListener(v -> cancel());
        findViewById(R.id.choose_photo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select image"), REQUEST_IMAGE);
        });
        lightButton.setOnClickListener(v -> toggleTorch());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            startCamera();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyTargetRotation();
        if (overlay != null) {
            overlay.invalidate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                failAndFinish("PERMISSION_NOT_GRANTED", "Camera permission denied");
            }
        }
    }

    private void startCamera() {
        if (cameraBound) {
            applyTargetRotation();
            return;
        }
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                if (!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    failAndFinish("CAMERA_START_FAILED", "No back camera");
                    return;
                }
                preview = new Preview.Builder()
                        .setTargetRotation(currentRotation())
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetRotation(currentRotation())
                        .build();
                analysis.setAnalyzer(cameraExecutor, image -> {
                    if (handled) {
                        image.close();
                        return;
                    }
                    String code = QrDecoder.decodeImageProxy(image);
                    image.close();
                    if (code != null && !handled) {
                        handled = true;
                        Log.i(TAG, "decoded length=" + code.length());
                        runOnUiThread(() -> finishWith(code));
                    }
                });

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                provider.unbindAll();
                camera = provider.bindToLifecycle(this, selector, preview, analysis);
                cameraBound = true;
                Log.i(TAG, "camera bound rotation=" + currentRotation()
                        + " preview=" + previewView.getWidth() + "x" + previewView.getHeight());
            } catch (Exception e) {
                Log.e(TAG, "camera start failed", e);
                failAndFinish(classifyCameraFailure(e), e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private static String classifyCameraFailure(Exception e) {
        String name = e.getClass().getSimpleName();
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (name.contains("Unavailable") || msg.contains("in use")
                || msg.contains("camera_in_use") || msg.contains("higher-priority")) {
            return "CAMERA_IN_USE";
        }
        return "CAMERA_START_FAILED";
    }

    private void applyTargetRotation() {
        int rotation = currentRotation();
        if (preview != null) {
            preview.setTargetRotation(rotation);
        }
        if (analysis != null) {
            analysis.setTargetRotation(rotation);
        }
    }

    private int currentRotation() {
        Display display = previewView != null ? previewView.getDisplay() : null;
        if (display == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display = getDisplay();
        }
        if (display == null) {
            display = getWindowManager().getDefaultDisplay();
        }
        return display != null ? display.getRotation() : Surface.ROTATION_0;
    }

    private void toggleTorch() {
        if (camera == null || camera.getCameraInfo().getTorchState() == null) {
            Toast.makeText(this, "Can't use light", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            isLightOpen = !isLightOpen;
            camera.getCameraControl().enableTorch(isLightOpen);
        } catch (Exception e) {
            isLightOpen = false;
            Toast.makeText(this, "Can't use light", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (overlay != null) {
            overlay.start();
        }
        if (sensorManager != null && lightSensor != null && sensorEventListener != null) {
            sensorManager.registerListener(sensorEventListener, lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        applyTargetRotation();
    }

    @Override
    protected void onPause() {
        if (overlay != null) {
            overlay.stop();
        }
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            String code = decodeUri(data.getData());
            finishWith(code);
        }
    }

    private String decodeUri(Uri uri) {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
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

    @Override
    public void onBackPressed() {
        cancel();
    }

    private void cancel() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void failAndFinish(String errorCode, String message) {
        Log.e(TAG, "fail " + errorCode + " " + message);
        Intent data = new Intent();
        data.putExtra(EXTRA_ERROR, errorCode);
        if (message != null) {
            data.putExtra(EXTRA_ERROR_MESSAGE, message);
        }
        setResult(Activity.RESULT_CANCELED, data);
        finish();
    }

    private void finishWith(String code) {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT, code);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        super.onDestroy();
    }
}
