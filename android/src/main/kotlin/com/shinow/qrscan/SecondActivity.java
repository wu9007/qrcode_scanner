package com.shinow.qrscan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;

import com.uuzuche.lib_zxing.activity.CaptureFragment;
import com.uuzuche.lib_zxing.activity.CodeUtils;

/**
 * 定制化显示扫描界面
 */
public class SecondActivity extends AppCompatActivity {

    private CaptureFragment captureFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        captureFragment = new CaptureFragment();
        CodeUtils.setFragmentArgs(captureFragment, R.layout.my_camera);
        captureFragment.setAnalyzeCallback(analyzeCallback);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_my_container, captureFragment).commit();

        initView();
    }

    public static boolean isOpen = false;

    private void initView() {
        LinearLayout lightLayout = (LinearLayout) findViewById(R.id.scan_light);
        LinearLayout backLayout = (LinearLayout) findViewById(R.id.scan_back);
        lightLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOpen) {
                    CodeUtils.isLightEnable(true);
                    isOpen = true;
                } else {
                    CodeUtils.isLightEnable(false);
                    isOpen = false;
                }

            }
        });
        backLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SecondActivity.this.finish();
            }
        });
    }

    CodeUtils.AnalyzeCallback analyzeCallback = new CodeUtils.AnalyzeCallback() {
        @Override
        public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_SUCCESS);
            bundle.putString(CodeUtils.RESULT_STRING, result);
            resultIntent.putExtras(bundle);
            SecondActivity.this.setResult(RESULT_OK, resultIntent);
            SecondActivity.this.finish();
        }

        @Override
        public void onAnalyzeFailed() {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_FAILED);
            bundle.putString(CodeUtils.RESULT_STRING, "");
            resultIntent.putExtras(bundle);
            SecondActivity.this.setResult(RESULT_OK, resultIntent);
            SecondActivity.this.finish();
        }
    };
}
