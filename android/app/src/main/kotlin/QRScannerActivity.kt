package com.example.android_qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QRScannerActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var autoZoomApplied = false
    private var isProcessing = false

    // Modern way to handle activity results
    private val browserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // When returning from browser, restart the camera
        restartCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        previewView = findViewById(R.id.preview_view)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
        } else {
            startCamera()
        }

        setupPinchToZoom()
    }

    private fun setupPinchToZoom() {
        val scaleGestureDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val currentZoom = camera?.cameraInfo?.zoomState?.value?.linearZoom ?: 0f
                    val scaleFactor = detector.scaleFactor
                    val sensitivity = 0.25f
                    val newZoom = (currentZoom + (scaleFactor - 1) * sensitivity).coerceIn(0f, 1f)
                    camera?.cameraControl?.setLinearZoom(newZoom)
                    return true
                }
            })

        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCamera()
            } catch (e: Exception) {
                Log.e("QRScanner", "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun bindCamera() {
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            if (isProcessing) {
                imageProxy.close()
                return@setAnalyzer
            }

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                BarcodeScanning.getClient().process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull()
                        val scannedValue = barcode?.rawValue

                        if (barcode != null && !scannedValue.isNullOrBlank() && !isProcessing) {
                            isProcessing = true

                            val boundingBox = barcode.boundingBox
                            if (boundingBox != null && !autoZoomApplied) {
                                val frameWidth = previewView.width
                                val frameHeight = previewView.height
                                val boxArea = boundingBox.width() * boundingBox.height()
                                val frameArea = frameWidth * frameHeight
                                val boxCoverage = boxArea.toFloat() / frameArea

                                val currentZoom = camera?.cameraInfo?.zoomState?.value?.linearZoom ?: 0.5f
                                if (boxCoverage < 0.2f) {
                                    val targetZoom = (currentZoom + 0.25f).coerceAtMost(1.0f)
                                    camera?.cameraControl?.setLinearZoom(targetZoom)
                                    autoZoomApplied = true

                                    previewView.postDelayed({
                                        handleScannedResult(scannedValue)
                                    }, 500)
                                    imageProxy.close()
                                    return@addOnSuccessListener
                                }
                            }

                            handleScannedResult(scannedValue)
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider?.unbindAll()
        camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
    }

    private fun handleScannedResult(scannedValue: String) {
        if (scannedValue.startsWith("http")) {
            // Launch browser and wait for result
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(scannedValue))
            browserLauncher.launch(browserIntent)
        } else {
            // Non-URL result, finish with result
            finishWithResult(scannedValue)
        }
    }

    private fun restartCamera() {
        // Reset flags
        autoZoomApplied = false
        isProcessing = false

        // Restart camera
        cameraProvider?.let {
            bindCamera()
        }
    }

    private fun finishWithResult(value: String?) {
        if (!value.isNullOrBlank()) {
            val resultIntent = Intent().apply {
                putExtra("qr_result", value)
            }
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset processing flag when activity resumes
        if (isProcessing) {
            isProcessing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}