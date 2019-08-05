package com.shinow.qrscan;

import android.graphics.Bitmap;
import android.content.Intent;
import io.flutter.plugin.common.MethodChannel.Result;
import com.uuzuche.lib_zxing.activity.CodeUtils;

public class CustomAnalyzeCallback implements CodeUtils.AnalyzeCallback {
    private Result result;
    private Intent intent;

    public CustomAnalyzeCallback(Result result, Intent intent) {
        this.result = result;
        this.intent = intent;
    }

    @Override
    public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
        this.result.success(result);
    }

    @Override
    public void onAnalyzeFailed() {
        String errorCode = this.intent.getStringExtra("ERROR_CODE");
        if (errorCode != null) {
            this.result.error(errorCode, null, null);
        }
    }
}