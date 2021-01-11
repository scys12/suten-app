package com.profexent.suten

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_capture.*
import kotlinx.android.synthetic.main.activity_tebak.*
import kotlinx.android.synthetic.main.bottom_sheet_layout.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.random.Random


class CaptureActivity : AppCompatActivity() {
    private lateinit var container: ConstraintLayout
    private lateinit var bitmapBuffer: Bitmap

    private val executor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val isFrontFacing get() = lensFacing == CameraSelector.LENS_FACING_FRONT
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider : ProcessCameraProvider
    private var pauseAnalysis = false
    private var imageRotationDegrees: Int = 0
    private val tfImageBuffer = TensorImage(DataType.UINT8)
    private var tfLiteClassifier: TFLiteClassifier = TFLiteClassifier(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        container = findViewById(R.id.camera_container)
        tfLiteClassifier
            .initialize(2)
            .addOnSuccessListener { }
            .addOnFailureListener { e -> Log.e(TAG, "Error in setting up the classifier.", e) }

        if (allPermissionsGranted()) {
            initCamera()
        } else {
            requestPermissions(permissions.toTypedArray(), permissionsRequestCode);
        }
    }

    private fun initCamera(){
        view_finder.post{
            cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProvider = cameraProviderFuture.get()
            cameraProviderFuture.addListener(Runnable {
                startCamera()
            }, ContextCompat.getMainExecutor(this))
        }
    }

    /** Declare and bind preview and analysis use cases */
    @SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
    private fun startCamera() {
        // Camera provider is now guaranteed to be available
        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = AspectRatio.RATIO_16_9
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(view_finder.display.rotation)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        camera_capture_button.setOnClickListener {
//            val mDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
//            val file: File =
//                File(getBatchDirectoryName(), mDateFormat.format(Date()).toString() + ".jpg")

//            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            it.isEnabled = false
            if (pauseAnalysis){
                pauseAnalysis = false
                runOnUiThread(Runnable {
                    image_predicted.visibility = View.GONE
                })
            }
            else{
                imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        // Disable all camera controls
                        // Otherwise, pause image analysis and freeze image
                        if (!pauseAnalysis) {
                            pauseAnalysis = true
                            val buffer = image.toBitmap()
                            runOnUiThread(Runnable {
                                val imagePredicted = findViewById<ImageView>(R.id.image_predicted)
                                imagePredicted.setImageBitmap(buffer)
                                imagePredicted.visibility = View.VISIBLE
                            })
                        }
                        image.close()
                    }
                })
            }
            it.isEnabled = true
        }

        // Set up the view finder use case to display camera preview
        val previewConfig = Preview.Builder().apply {
            setCameraSelector(cameraSelector)
            setTargetAspectRatio(screenAspectRatio)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display?.let { setTargetRotation(it.rotation) }
            }
            setTargetRotation(view_finder.display.rotation)
        }.build().also {
            it.setSurfaceProvider(view_finder.surfaceProvider)
        }

        // Set up the image analysis use case which will process frames in real time
        val analyzerConfig = ImageAnalysis.Builder()
            .setTargetResolution(screenSize)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setCameraSelector(cameraSelector)
            .build()

        analyzerConfig.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
            tfLiteClassifier
                .classifyAsync(image.bitmap())
                .addOnSuccessListener { resultText ->
                    reportPrediction(resultText)
                }
                .addOnFailureListener { error -> }
            image.close()
//            if (pauseAnalysis) {
//                image.close()
//                return@Analyzer
//            }
//            var frameCounter = 0
//            var lastFpsTimestamp = System.currentTimeMillis()
//            val frameCount = 10
//            if (++frameCounter % frameCount == 0) {
//                frameCounter = 0
//                val now = System.currentTimeMillis()
//                val delta = now - lastFpsTimestamp
//                val fps = 1000 * frameCount.toFloat() / delta
//                Log.d(TAG, "FPS: ${"%.02f".format(fps)}")
//                lastFpsTimestamp = now
//            }
        })

        // Apply declared configs to CameraX using the same lifecycle owner
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner, cameraSelector, previewConfig, analyzerConfig, imageCapture
        )

        // Use the camera object to link our preview use case with the view
    }

    fun getBatchDirectoryName(): String? {
        var app_folder_path = ""
        app_folder_path =
            Environment.getStorageDirectory().toString() + "/images"
        val dir = File(app_folder_path)
        if (!dir.exists() && !dir.mkdirs()) {
        }
        return app_folder_path
    }


    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )

    private fun reportPrediction(
        prediction: TFLiteClassifier.DataOutput?
    )  {

        // Early exit: if prediction is not good enough, don't report it
        if (prediction == null || prediction.score < ACCURACY_THRESHOLD) {
            box_prediction.visibility = View.GONE
            text_prediction.visibility = View.GONE
        }else{
            text_prediction.text = "${"%.2f".format(prediction.score)} ${prediction.label}"
            // Make sure all UI elements are visible
            box_prediction.visibility = View.VISIBLE
            text_prediction.visibility = View.VISIBLE
        }

        // Location has to be mapped to our local coordinates
//        val location = mapOutputCoordinates(prediction.location)

        // Update the text and UI
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == permissionsRequestCode) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        tfLiteClassifier.close()
        super.onDestroy()
    }

    private fun allPermissionsGranted(): Boolean {

        for (permission in permissions) {
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

    companion object {
        private val TAG = CaptureActivity::class.java.simpleName

        private const val ACCURACY_THRESHOLD = 0.5f
        private const val MODEL_PATH = "coco_ssd_mobilenet_v1_1.0_quant.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}

private fun ImageProxy.bitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val uBuffer = planes[1].buffer // U
    val vBuffer = planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer: ByteBuffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    val clonedBytes = bytes.clone()
    return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.size)
}
