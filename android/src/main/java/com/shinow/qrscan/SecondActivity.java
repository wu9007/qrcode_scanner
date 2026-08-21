package com.shinow.qrscan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
    public static boolean isLightOpen = false;

    private static final int REQUEST_IMAGE = 101;
    private static final int REQUEST_CAMERA = 202;

    private PreviewView previewView;
    private ScanOverlayView overlay;
    private ImageView lightButton;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private volatile boolean handled = false;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener sensorEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isLightOpen = false;
        setContentView(R.layout.activity_second);
        previewView = findViewById(R.id.preview_view);
        overlay = findViewById(R.id.scan_overlay);
        lightButton = findViewById(R.id.scan_light);
        cameraExecutor = Executors.newSingleThreadExecutor();

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Intent data = new Intent();
                data.putExtra("ERROR_CODE", "PERMISSION_NOT_GRANTED");
                setResult(Activity.RESULT_CANCELED, data);
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
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
                        runOnUiThread(() -> finishWith(code));
                    }
                });

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                provider.unbindAll();
                camera = provider.bindToLifecycle(this, selector, preview, analysis);
            } catch (Exception e) {
                Toast.makeText(this, "Camera start failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
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
