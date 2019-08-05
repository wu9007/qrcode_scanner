package com.shinow.qrscan

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import com.uuzuche.lib_zxing.activity.CodeUtils
import com.uuzuche.lib_zxing.activity.CodeUtils.RESULT_SUCCESS
import com.uuzuche.lib_zxing.activity.CodeUtils.RESULT_TYPE
import com.uuzuche.lib_zxing.activity.ZXingLibrary

import  com.shinow.qrscan.ImageUtil

/**
 * @author shusheng 2018/9/30
 */
class QrscanPlugin(val activity: Activity) : MethodCallHandler,
        PluginRegistry.ActivityResultListener {
    private var result: Result? = null
    val REQUEST_CODE = 100
    val REQUEST_IMAGE = 101

    companion object {
        /**
         * registrar channel plugin
         */
        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            val channel = MethodChannel(registrar.messenger(), "qr_scan")
            val plugin = QrscanPlugin(registrar.activity())
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)

            ZXingLibrary.initDisplayOpinion(registrar.activity())
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        if (call.method == "scan") {
            this.result = result
            showBarcodeView()
        } else if (call.method == "scan_photo") {
            this.result = result
            choosePhotos()
        } else {
            result.notImplemented()
        }
    }

    private fun showBarcodeView() {
        val intent = Intent(activity, SecondActivity::class.java)
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    private fun choosePhotos() {
        val intent = Intent()
        intent.setAction(Intent.ACTION_PICK)
        intent.setType("image/*")
        activity.startActivityForResult(intent, REQUEST_IMAGE)
    }

    override fun onActivityResult(code: Int, resultCode: Int, intent: Intent?): Boolean {
        if (code == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                val bundle = intent.extras
                if (bundle != null) {
                    if (bundle.getInt(RESULT_TYPE) == RESULT_SUCCESS) {
                        val barcode = bundle.getString(CodeUtils.RESULT_STRING)
                        barcode?.let {
                            this.result?.success(barcode)
                        }
                    }
                }
            } else {
                val errorCode = intent?.getStringExtra("ERROR_CODE")
                if (errorCode != null) {
                    this.result?.error(errorCode, null, null)
                }
            }
            return true
        } else if (code == REQUEST_IMAGE) {
            if (intent != null) {
                val uri = intent.getData()
                try {
                    var analyzeCallback: CodeUtils.AnalyzeCallback? = CustomAnalyzeCallback(this.result, intent);
                    CodeUtils.analyzeBitmap(ImageUtil.getImageAbsolutePath(activity, uri), analyzeCallback)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return true
        }
        return false
    }
}
