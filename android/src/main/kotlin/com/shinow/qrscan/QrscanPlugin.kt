package com.shinow.qrscan

import android.app.Activity
import android.content.Intent
import com.uuzuche.lib_zxing.activity.CaptureActivity
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

class QrscanPlugin(val activity: Activity) : MethodCallHandler,
        PluginRegistry.ActivityResultListener {
  private var result: Result? = null

  companion object {
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
    } else {
      result.notImplemented()
    }
  }

  private fun showBarcodeView() {
    val intent = Intent(activity, CaptureActivity::class.java)
    activity.startActivityForResult(intent, 100)
  }

  override fun onActivityResult(code: Int, resultCode: Int, data: Intent?): Boolean {
    if (code == 100) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        val bundle = data.extras
        if (bundle != null) {
          if (bundle.getInt(RESULT_TYPE) == RESULT_SUCCESS) {
            val barcode = bundle.getString(CodeUtils.RESULT_STRING)
            barcode?.let {
              this.result?.success(barcode)
            }
          }
        }
      } else {
        val errorCode = data?.getStringExtra("ERROR_CODE")
        this.result?.error(errorCode, null, null)
      }
      return true
    }
    return false
  }
}
