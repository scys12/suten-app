package com.profexent.suten

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.ImageReaderProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.makeramen.roundedimageview.RoundedImageView
import kotlinx.android.synthetic.main.activity_tebak.*
import org.w3c.dom.Text
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class TebakActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider : ProcessCameraProvider
    private val REQUEST_CODE_PERMISSIONS = 101
    private val REQUIRED_PERMISSIONS = arrayOf("android.permission.CAMERA")

    private var tfLiteClassifier: TFLiteClassifier = TFLiteClassifier(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tebak)

        tfLiteClassifier
            .initialize(1)
            .addOnSuccessListener { }
            .addOnFailureListener { e -> Log.e(TAG, "Error in setting up the classifier.", e) }

        if (allPermissionsGranted()) {
            initCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

    }

    private fun initCamera(){
        textureView.post{
            cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProvider = cameraProviderFuture.get()
            cameraProviderFuture.addListener(Runnable {
                startCamera()
            }, ContextCompat.getMainExecutor(this))
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val metrics = DisplayMetrics().also { textureView.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = AspectRatio.RATIO_16_9
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val previewConfig = Preview.Builder().apply {
            setCameraSelector(cameraSelector)
            setTargetAspectRatio(screenAspectRatio)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display?.let { setTargetRotation(it.rotation) }
            }
            setTargetRotation(textureView.display.rotation)
        }.build().also {
            it.setSurfaceProvider(textureView.surfaceProvider)
        }
        val textView = findViewById<TextView>(R.id.predictedTextView)
        val imageView = findViewById<RoundedImageView>(R.id.predictedImage)
        val analysis = ImageAnalyzer(tfLiteClassifier, textView, imageView)

        val analyzerConfig = ImageAnalysis.Builder()
            .setTargetResolution(screenSize)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setCameraSelector(cameraSelector)
            .build()
            .apply {
                setAnalyzer(Executors.newSingleThreadExecutor(), analysis)
            }
        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, previewConfig, analyzerConfig)
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = textureView.width / 2f
        val centerY = textureView.height / 2f

        val rotationDegrees = when (textureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {

        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onDestroy() {
        tfLiteClassifier.close()
        super.onDestroy()
    }


}