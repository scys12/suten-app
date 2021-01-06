package com.profexent.suten

import android.graphics.*
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxyProvider
import androidx.camera.core.impl.ImageReaderProxy
import com.makeramen.roundedimageview.RoundedImageView
import kotlinx.android.synthetic.main.activity_tebak.*
import java.io.ByteArrayOutputStream

class ImageAnalyzer (var tfLiteClassifier: TFLiteClassifier, var predictedTextView: TextView?, var predictedImage: RoundedImageView) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val bitmap = image.toBitmap()

        tfLiteClassifier
            .classifyAsync(bitmap)
            .addOnSuccessListener { resultText ->
                predictedTextView?.text = resultText
                if (resultText.equals("rock")){
                    predictedImage.setImageResource(R.drawable.rock)
                }else if (resultText.equals("paper")){
                    predictedImage.setImageResource(R.drawable.paper)
                }else if (resultText.equals("scissors")){
                    predictedImage.setImageResource(R.drawable.scissor)
                }
            }
            .addOnFailureListener { error ->  }
        image.close()
    }

    fun ImageProxy.toBitmap(): Bitmap {
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

}