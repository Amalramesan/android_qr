package com.example.android_qr

import android.app.Activity
import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.qrcode_scanner/methods"
    private val REQUEST_CODE_SCAN = 101

    private var resultCallback: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
                call, result ->
            if (call.method == "startScanner") {
                resultCallback = result
                val intent = Intent(this, QRScannerActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_SCAN)
            } else {
                result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            val scannedValue = data?.getStringExtra("qr_result")
            resultCallback?.success(scannedValue)
        } else {
            resultCallback?.error("CANCELLED", "User cancelled", null)
        }
    }
}
