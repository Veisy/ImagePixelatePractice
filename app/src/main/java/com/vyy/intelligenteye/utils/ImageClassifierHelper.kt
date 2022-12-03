package com.vyy.intelligenteye.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class ImageClassifierHelper(
    private val context: Context
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(MAX_RESULTS)

        val baseOptionsBuilder = BaseOptions.builder()

        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            baseOptionsBuilder.useGpu()
        } else {
            Log.e(TAG, "GPU is not supported on this device")
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName = "eye_model.tflite"

        try {
            imageClassifier =
                ImageClassifier.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun classify(image: Bitmap): MutableList<Classifications>? {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Create preprocessor for the image.
        val imageProcessor =
            ImageProcessor.Builder().build()

        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        return imageClassifier?.classify(tensorImage)
    }

    companion object {
        const val MAX_RESULTS = 1

        private const val TAG = "ImageClassifierHelper"
    }
}