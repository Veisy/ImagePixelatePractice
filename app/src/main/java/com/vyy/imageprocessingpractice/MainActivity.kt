package com.vyy.imageprocessingpractice

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.vyy.imagemosaicing.R
import com.vyy.imagemosaicing.databinding.ActivityMainBinding
import com.vyy.imageprocessingpractice.processes.*
import com.vyy.imageprocessingpractice.utils.checkEnoughTimePassed
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayDeque


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var imageUri: Uri? = null
    private var imageBitmap: Bitmap? = null
    private var imageStack: ArrayDeque<Bitmap> = ArrayDeque()

    private var imageProcessingJob: Job? = null
    private var checkGrayScaleJob: Job? = null
    private var imageUriToBitmapDeferred: Deferred<Bitmap?>? = null

    private var selectedProcess: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerActivityResultCallbacks()
        setClickListeners()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStart() {
        super.onStart()

        checkPermissions()

        // Default Image is Lenna.
        if (imageBitmap == null || imageStack.size < 1) {
            imageUri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(R.drawable.lenna))
                .appendPath(resources.getResourceTypeName(R.drawable.lenna))
                .appendPath(resources.getResourceEntryName(R.drawable.lenna))
                .build()
            updateImageView(imageUri)
            hideGrayAndRgbTextView()

            imageUriToBitmapDeferred = this.lifecycleScope.async(Dispatchers.Default) {
                val bitmap = imageUri?.let { uriToBitmap(it) }
                if (bitmap != null) {
                    addToImageStack(bitmap)
                }
                bitmap
            }
        }
    }

    private fun checkPermissions() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setClickListeners() {
        binding.apply {
            cameraButton.setOnClickListener(this@MainActivity)
            buttonProcess.setOnClickListener(this@MainActivity)
            galleryButton.setOnClickListener(this@MainActivity)
            textViewRgb.setOnClickListener(this@MainActivity)
            imageButtonReflectYAxis.setOnClickListener(this@MainActivity)
            imageButtonReflectXAxis.setOnClickListener(this@MainActivity)
            imageButtonResize.setOnClickListener(this@MainActivity)
            imageButtonCrop.setOnClickListener(this@MainActivity)
            imageButtonPixelate.setOnClickListener(this@MainActivity)
            imageButtonUndo.setOnClickListener(this@MainActivity)
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (v.id) {
                R.id.imageButton_undo -> {
                    cancelCurrentJobs()
                    hideGrayAndRgbTextView()
                    removeFromImageStack()
                }

                R.id.cameraButton -> {
                    cancelCurrentJobs()
                    hideGrayAndRgbTextView()
                    takePhoto()
                }

                R.id.galleryButton -> {
                    cancelCurrentJobs()
                    hideGrayAndRgbTextView()
                    pickPhoto()
                }

                R.id.textView_rgb -> {
                    cancelCurrentJobs(isImageUriToBitmapCanceled = false)
                    imageProcessingJob = this.lifecycleScope.launch(Dispatchers.Main) {
                        convertToGrayScale()
                    }
                }

                R.id.imageButton_reflect_y_axis -> {
                    cancelCurrentJobs(
                        isCheckGrayScaleCanceled = false,
                        isImageUriToBitmapCanceled = false
                    )

                    imageProcessingJob = this.lifecycleScope.launch(Dispatchers.Main) {
                        reflectBitmap(isReflectOnXAxis = false)
                    }
                }

                R.id.imageButton_reflect_x_axis -> {
                    cancelCurrentJobs(
                        isCheckGrayScaleCanceled = false,
                        isImageUriToBitmapCanceled = false
                    )

                    imageProcessingJob = this.lifecycleScope.launch(Dispatchers.Main) {
                        reflectBitmap(isReflectOnXAxis = true)
                    }
                }

                R.id.imageButton_resize -> {
                    updateSelectedProcess(binding.imageButtonResize)
                }

                R.id.imageButton_crop -> {
                    updateSelectedProcess(binding.imageButtonCrop)
                }
                R.id.imageButton_pixelate -> {
                    updateSelectedProcess(binding.imageButtonPixelate)
                }

                R.id.button_process -> {
                    if (selectedProcess == R.id.imageButton_resize
                        || selectedProcess == R.id.imageButton_pixelate
                    ) {
                        val width = binding.textInputEditTextWidth.text.toString()
                        val height = binding.textInputEditTextHeight.text.toString()
                        if (checkIfInputsValid(listOf(width, height))) {
                            cancelCurrentJobs(
                                isCheckGrayScaleCanceled = false,
                                isImageUriToBitmapCanceled = false
                            )

                            imageProcessingJob = this.lifecycleScope.launch(Dispatchers.Main) {
                                when (selectedProcess) {
                                    R.id.imageButton_resize -> resizeBitmap(
                                        width.toDouble(),
                                        height.toDouble()
                                    )
                                    R.id.imageButton_pixelate -> pixelateBitmap(
                                        width.toDouble(),
                                        height.toDouble()
                                    )
                                }

                            }
                        }
                    } else if (selectedProcess == R.id.imageButton_crop) {
                        val fromX = binding.textInputEditTextFromX.text.toString()
                        val fromY = binding.textInputEditTextFromY.text.toString()
                        val toX = binding.textInputEditTextToX.text.toString()
                        val toY = binding.textInputEditTextToY.text.toString()
                        if (checkIfInputsValid(listOf(fromX, fromY, toX, toY))) {
                            cancelCurrentJobs(
                                isCheckGrayScaleCanceled = false,
                                isImageUriToBitmapCanceled = false
                            )

                            imageProcessingJob = this.lifecycleScope.launch(Dispatchers.Main) {
                                cropBitmap(
                                    fromX.toDouble(),
                                    fromY.toDouble(),
                                    toX.toDouble(),
                                    toY.toDouble()
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun cancelCurrentJobs(
        isImageProcessingCanceled: Boolean = true,
        isCheckGrayScaleCanceled: Boolean = true,
        isImageUriToBitmapCanceled: Boolean = true
    ) {
        if (isImageProcessingCanceled && imageProcessingJob?.isActive == true)
            imageProcessingJob?.cancel()
        if (isCheckGrayScaleCanceled && checkGrayScaleJob?.isActive == true)
            checkGrayScaleJob?.cancel()
        if (isImageUriToBitmapCanceled && imageUriToBitmapDeferred?.isActive == true)
            imageUriToBitmapDeferred?.cancel()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
        ) {
            // Camera permission is granted, start camera.
            startCamera()
        }
    }

    private fun pickPhoto() {
        pickMedia?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun registerActivityResultCallbacks() {
        // Registers a photo picker activity launcher in single-select mode.
        pickMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d(TAG, "Selected URI: $uri")
                    try {
                        imageUri = uri
                        updateImageView(uri)

                        imageUriToBitmapDeferred = this.lifecycleScope.async(Dispatchers.Default) {
                            val bitmap = imageUri?.let { uriToBitmap(it) }
                            if (bitmap != null) {
                                addToImageStack(bitmap)
                            }
                            bitmap
                        }

                        checkIfGrayScale()
                    } catch (e: Exception) {
                        Log.e(TAG, "Picking image from media failed: ${e.message}", e)
                    }
                } else {
                    Log.d(TAG, "No media selected")
                }
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Set up the capture use case to allow users to take photos.
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Camera use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Take photo when camera button clicked.
    private fun takePhoto() {
        // If camera permission is not granted, return.
        if (!allPermissionsGranted()) {
            return
        }
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        showProgressDialog(true)

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()


        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        showProgressDialog(false)
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        // Photo taken from the Camera,
                        // show it in ImageView.
                        imageUri = outputFileResults.savedUri
                        runOnUiThread {
                            updateImageView(imageUri)
                        }

                        imageUriToBitmapDeferred = CoroutineScope(Dispatchers.Default).async {
                            val bitmap = imageUri?.let { uriToBitmap(it) }
                            if (bitmap != null) {
                                addToImageStack(bitmap)
                            }
                            bitmap
                        }

                        // Check if image is grayscale.
                        checkIfGrayScale()
                    } catch (e: Exception) {
                        Log.e(TAG, "Loading image uri to ImageView failed: ${e.message}", e)
                    } finally {
                        runOnUiThread {
                            showProgressDialog(false)
                        }
                    }
                }
            })
    }

    // Decode Uri to Bitmap, and then use pixelate algorithm on the Bitmap.
    private suspend fun pixelateBitmap(width: Double, height: Double) {
        try {
            showProgressDialog(true)
            imageBitmap = imageUriToBitmapDeferred?.await()

            // Since this operation takes time, we use Dispatchers.Default,
            // which is optimized for time consuming calculations.
            if (checkEnoughTimePassed() && imageBitmap != null) {
                val pixelatedBitmapDrawable = withContext(Dispatchers.Default) {
                    invokePixelation(
                        bitmap = imageBitmap!!,
                        pixelWidth = (width.toInt()).coerceAtMost(imageBitmap!!.width),
                        pixelHeight = (height.toInt()).coerceAtMost(imageBitmap!!.height),
                        resources = resources
                    )
                }

                updateImageView(pixelatedBitmapDrawable)

                imageUriToBitmapDeferred = CoroutineScope(Dispatchers.Default).async {
                    val bitmap = pixelatedBitmapDrawable.bitmap
                    addToImageStack(bitmap)
                    bitmap
                }
            } else {
                Log.e(
                    TAG,
                    "Not enough time has passed to re-pixate, or ImageBitmap is null."
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Pixelating bitmap failed: ${e.message}", e)
        } finally {
            showProgressDialog(false)
        }
    }

    private suspend fun reflectBitmap(isReflectOnXAxis: Boolean) {
        try {
            showProgressDialog(true)
            imageBitmap = imageUriToBitmapDeferred?.await()

            if (checkEnoughTimePassed() && imageBitmap != null) {
                // Since this operation takes time, we use Dispatchers.Default,
                // which is optimized for time consuming calculations.
                val reflectedBitmapDrawable = withContext(Dispatchers.Default) {
                    if (isReflectOnXAxis) {
                        reflectOnXAxis(
                            bitmap = imageBitmap!!,
                            resources = resources
                        )
                    } else {
                        reflectOnYAxis(
                            bitmap = imageBitmap!!,
                            resources = resources
                        )
                    }
                }

                updateImageView(reflectedBitmapDrawable)

                imageUriToBitmapDeferred = CoroutineScope(Dispatchers.Default).async {
                    val bitmap = reflectedBitmapDrawable.bitmap
                    addToImageStack(bitmap)
                    bitmap
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reflecting bitmap failed: ${e.message}", e)
        } finally {
            showProgressDialog(false)
        }
    }

    private suspend fun resizeBitmap(widthRatio: Double, heightRatio: Double) {
        try {
            showProgressDialog(true)
            imageBitmap = imageUriToBitmapDeferred?.await()

            if (
                checkEnoughTimePassed()
                && imageBitmap != null
                && widthRatio <= 1
                && heightRatio <= 1
            ) {
                // Since this operation takes time, we use Dispatchers.Default,
                // which is optimized for time consuming calculations.
                val resizedBitmapDrawable = withContext(Dispatchers.Default) {
                    resize(
                        bitmap = imageBitmap!!,
                        width = (widthRatio * imageBitmap!!.width).toInt(),
                        height = (heightRatio * imageBitmap!!.height).toInt(),
                        resources = resources
                    )
                }

                updateImageView(resizedBitmapDrawable)

                imageUriToBitmapDeferred = CoroutineScope(Dispatchers.Default).async {
                    val bitmap = resizedBitmapDrawable.bitmap
                    addToImageStack(bitmap)
                    bitmap
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Resizing bitmap failed: ${e.message}", e)
        } finally {
            showProgressDialog(false)
        }
    }

    private suspend fun cropBitmap(
        fromXRatio: Double,
        fromYRatio: Double,
        toXRatio: Double,
        toYRatio: Double
    ) {
        try {
            showProgressDialog(true)
            imageBitmap = imageUriToBitmapDeferred?.await()
            // X and Y points are taken proportional to the width and height of the bitmap
            if (
                checkEnoughTimePassed()
                && imageBitmap != null
                && fromXRatio <= 1
                && fromYRatio <= 1
                && toXRatio <= 1
                && toYRatio <= 1
                && fromXRatio < toXRatio
                && fromYRatio < toYRatio
            ) {
                // Since this operation takes time, we use Dispatchers.Default,
                // which is optimized for time consuming calculations.
                val croppedBitmapDrawable = withContext(Dispatchers.Default) {
                    crop(
                        bitmap = imageBitmap!!,
                        fromX = (fromXRatio * imageBitmap!!.width).toInt(),
                        fromY = (fromYRatio * imageBitmap!!.height).toInt(),
                        toX = (toXRatio * imageBitmap!!.width).toInt(),
                        toY = (toXRatio * imageBitmap!!.height).toInt(),
                        resources = resources
                    )
                }

                updateImageView(croppedBitmapDrawable)

                imageUriToBitmapDeferred = CoroutineScope(Dispatchers.Default).async {
                    val bitmap = croppedBitmapDrawable.bitmap
                    addToImageStack(bitmap)
                    bitmap
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cropping bitmap failed: ${e.message}", e)
        } finally {
            showProgressDialog(false)
        }
    }

    private suspend fun convertToGrayScale() {
        try {
            showProgressDialog(true)
            imageBitmap = imageUriToBitmapDeferred?.await()

            if (checkEnoughTimePassed() && imageBitmap != null) {
                withContext(Dispatchers.Default) {
                    val grayScaleBitmapDrawable =
                        convertToGrayScale(
                            bitmap = imageBitmap!!,
                            resources = resources
                        )

                    imageUriToBitmapDeferred = CoroutineScope(Dispatchers.Default).async {
                        val bitmap = grayScaleBitmapDrawable.bitmap
                        addToImageStack(bitmap)
                        bitmap
                    }

                    // Since we are doing UI operations at this line,
                    // we return back to Main Dispatcher.
                    withContext(Dispatchers.Main) {
                        updateImageView(grayScaleBitmapDrawable)
                        binding.apply {
                            textViewRgb.visibility = View.GONE
                            textViewGrayScale.visibility = View.VISIBLE
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Converting image to gray scale failed: ${e.message}", e)
        } finally {
            showProgressDialog(false)
        }
    }

    private fun checkIfGrayScale() {
        checkGrayScaleJob = this.lifecycleScope.launch {
            imageBitmap = imageUriToBitmapDeferred?.await()
            imageBitmap?.let { bitmap ->
                withContext(Dispatchers.Default) {
                    val isGrayScale = isGrayScale(bitmap)

                    withContext(Dispatchers.Main) {
                        binding.apply {
                            textViewGrayScale.visibility =
                                if (isGrayScale) View.VISIBLE else View.GONE
                            textViewRgb.visibility = if (!isGrayScale) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun checkIfInputsValid(inputs: List<String>) = inputs.all { input ->
        input.isNotEmpty() && input.toDouble() > 0
    }

    // Load image to imageView
    private fun updateImageView(image: Any?) {
        if (image is Uri || image is BitmapDrawable) {
            image.let {
                Glide
                    .with(this)
                    .load(it)
                    .into(binding.imageView)
            }
        }
    }

    private suspend fun addToImageStack(bitmap: Bitmap) {
        // If stack size is already 10, remove the first element
        if (imageStack.size == 10) {
            imageStack.removeAt(0)
        }

        imageStack.addLast(bitmap)
        if (imageStack.size > 1) {
            withContext(Dispatchers.Main) {
                binding.imageButtonUndo.visibility = View.VISIBLE
            }
        }
    }

    private fun removeFromImageStack() {
        if (imageStack.size > 1) {
            imageStack.removeLast()
            updateImageView(imageStack.last().toDrawable(resources))
            imageUriToBitmapDeferred = this.lifecycleScope.async(Dispatchers.Default) {
                imageStack.last()
            }
        } else if(imageStack.size == 1) {
            binding.imageButtonUndo.visibility = View.GONE
        }
    }

    private fun updateSelectedProcess(imageButton: ImageButton) {
        val allImageButtons = listOf(
            binding.imageButtonPixelate,
            binding.imageButtonResize,
            binding.imageButtonCrop
        )
        selectedProcess = imageButton.id

        binding.buttonProcess.apply {
            text = when (imageButton.id) {
                R.id.imageButton_resize -> getString(R.string.resize)
                R.id.imageButton_crop -> getString(R.string.crop)
                R.id.imageButton_pixelate -> getString(R.string.pixelate)
                else -> {
                    text.toString()
                }
            }
        }

        allImageButtons.forEach { button ->
            button.background = if (button == imageButton) {
                ContextCompat.getDrawable(this, R.drawable.image_button_selected_background)
            } else {
                ContextCompat.getDrawable(this, R.drawable.image_button_unselected_background)
            }
        }

        val cropInputLayouts = listOf(
            binding.textInputLayoutFromX,
            binding.textInputLayoutFromY,
            binding.textInputLayoutToX,
            binding.textInputLayoutToY
        )
        val widthAndHeightLayouts = listOf(
            binding.textInputLayoutWidth,
            binding.textInputLayoutHeight

        )
        cropInputLayouts.forEach {
            it.visibility = if (imageButton.id == R.id.imageButton_crop) View.VISIBLE else View.GONE
        }
        widthAndHeightLayouts.forEach {
            it.visibility = if (imageButton.id == R.id.imageButton_crop) View.GONE else View.VISIBLE
        }

        if (imageButton.id == R.id.imageButton_pixelate
            || imageButton.id == R.id.imageButton_crop
            || imageButton.id == R.id.imageButton_resize
        ) {
            binding.apply {
                buttonProcess.visibility = View.VISIBLE
                viewSeparatorLineVertical.visibility = View.VISIBLE
            }
        } else {
            binding.apply {
                buttonProcess.visibility = View.GONE
                viewSeparatorLineVertical.visibility = View.GONE
            }
        }
    }

    private fun hideGrayAndRgbTextView() {
        binding.apply {
            textViewGrayScale.visibility = View.GONE
            textViewRgb.visibility = View.GONE
        }
    }

    private fun showProgressDialog(isShown: Boolean) {
        binding.apply {
            progresBar.visibility = if (isShown) View.VISIBLE else View.GONE
            val clickableViews = listOf(
                buttonProcess,
                cameraButton,
                galleryButton,
                textViewRgb,
                imageButtonReflectYAxis,
                imageButtonReflectXAxis,
                imageButtonResize,
                imageButtonCrop,
                imageButtonPixelate
            )
            clickableViews.forEach { it.isEnabled = !isShown }
        }
    }

    // Decode image Uri to Bitmap
    private fun uriToBitmap(uri: Uri) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(
                contentResolver,
                uri
            )
        ) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(contentResolver, uri)
    }

    override fun onStop() {
        super.onStop()
        cancelCurrentJobs()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}