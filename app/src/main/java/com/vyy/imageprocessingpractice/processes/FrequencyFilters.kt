package com.vyy.imageprocessingpractice.processes

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable
import com.vyy.imageprocessingpractice.utils.lastProcessTime
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Core.polarToCart
import org.opencv.imgproc.Imgproc


// Apply a high pass filter to the Bitmap image with using openCV
fun applyHighPassFilter(bitmap: Bitmap, resources: Resources): BitmapDrawable {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    val filteredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val mat = Mat()
    Utils.bitmapToMat(filteredBitmap, mat)
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
    val dst = Mat()
    Imgproc.Laplacian(mat, dst, -1)
    Utils.matToBitmap(dst, filteredBitmap)
    return filteredBitmap.toDrawable(resources)
}

/** Take Discrete Fourier Transform of the image
    * and then shift the zero-frequency component to the center of the spectrum.
    * Compute the magnitude spectrum of the image
    * Apply mask to filter noise
    * shift the zero-frequency component to the original position
    * Take inverse Discrete Fourier Transform
    */
fun specialFrequencyOperation(bitmap: Bitmap, resources: Resources): List<BitmapDrawable> {
    // We use this variable to check if enough time has passed for a new operation.
    lastProcessTime = System.currentTimeMillis()

    // Store intermediate images after every step.
    val intermediateBitmapDrawables = mutableListOf<BitmapDrawable>()

    val image = Mat()
    Utils.bitmapToMat(bitmap, image)
    Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY)

    // Add padding to the image
    val padded = Mat()
    val m = Core.getOptimalDFTSize(image.rows())
    val n = Core.getOptimalDFTSize(image.cols())
    Core.copyMakeBorder(image, padded, 0, m - image.rows(), 0, n - image.cols(), Core.BORDER_CONSTANT, Scalar.all(0.0))

    val paddedBitmap = Bitmap.createBitmap(padded.cols(), padded.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(padded, paddedBitmap)
    intermediateBitmapDrawables.add(paddedBitmap.toDrawable(resources))

    // The result of the DFT is complex so we have to make place for both the complex and the real values.
    // We store these usually at least in a float format.
    // Therefore we’ll convert our input image to this type and expand it with another channel to hold the complex values
    padded.convertTo(padded, CvType.CV_32F)
    val planes = ArrayList<Mat>()
    planes.add(padded)
    planes.add(Mat.zeros(padded.size(), CvType.CV_32F))
    val complexImage = Mat()
    Core.merge(planes, complexImage)

    // Compute the DFT
    Core.dft(complexImage, complexImage)
    Core.split(complexImage, planes)

    val magnitude = Mat()
    Core.magnitude(planes[0], planes[1], magnitude)
    val phase = Mat()
    Core.phase(planes[0], planes[1], phase)

    intermediateBitmapDrawables.addMat(magnitude, resources)

    // Throw away the newly introduced values. Shift the zero-frequency component to the center of the spectrum
    shiftZeroFrequencyComponent(magnitude, magnitude)

    // Store shifted magnitude
    intermediateBitmapDrawables.addMat(magnitude, resources)

    // Apply mask
    val mask = Mat(magnitude.size(), CvType.CV_32F, Scalar(0.0))
    val center = Point(mask.cols() / 2.0, mask.rows() / 2.0)
    val radius = 40.0
    Imgproc.circle(mask, center, radius.toInt(), Scalar(1.0), -1, 8, 0)
    Core.multiply(magnitude, mask, magnitude)

    // Store masked/shifted magnitude
    intermediateBitmapDrawables.addMat(magnitude, resources)

    // Shift the zero-frequency component to the original position
    shiftZeroFrequencyComponent(magnitude, magnitude)

    // Store masked magnitude
    intermediateBitmapDrawables.addMat(magnitude, resources)

    // With this magnitude and phase obtain new complex image
    polarToCart(magnitude, phase, planes[0], planes[1])
    val newComplexImage = Mat()
    Core.merge(listOf(planes[0], planes[1]), newComplexImage)

    Core.idft(newComplexImage, newComplexImage)

    Core.split(newComplexImage, planes)

    // Cut the padded part
    planes[0].submat(Rect(0, 0, image.cols(), image.rows())).copyTo(planes[0])

    intermediateBitmapDrawables.addMat(planes[0], resources)
    return intermediateBitmapDrawables
}

// Shift/Restore the zero-frequency component to the center of the spectrum
private fun shiftZeroFrequencyComponent(src: Mat, dst: Mat) {
    val crop = Rect(0, 0, src.cols() and -2, src.rows() and -2)
    val q0 = src.submat(Rect(0, 0, crop.width / 2, crop.height / 2))
    val q1 = src.submat(Rect(crop.width / 2, 0, crop.width / 2, crop.height / 2))
    val q2 = src.submat(Rect(0, crop.height / 2, crop.width / 2, crop.height / 2))
    val q3 = src.submat(Rect(crop.width / 2, crop.height / 2, crop.width / 2, crop.height / 2))
    val tmp = Mat()
    q0.copyTo(tmp)
    q3.copyTo(q0)
    tmp.copyTo(q3)
    q1.copyTo(tmp)
    q2.copyTo(q1)
    tmp.copyTo(q2)
    src.copyTo(dst)
}

// Add Mat() to the list of BitmapDrawables
private fun MutableList<BitmapDrawable>.addMat(mat: Mat, resources: Resources) {
    val intermediateMat = Mat()
    mat.copyTo(intermediateMat)
    Core.add(Mat.ones(intermediateMat.size(), CvType.CV_32F), intermediateMat, intermediateMat)
    Core.log(intermediateMat, intermediateMat)
    intermediateMat.convertTo(intermediateMat, CvType.CV_8UC1)
    Core.normalize(intermediateMat, intermediateMat, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)
    val intermediateBitmap = Bitmap.createBitmap(intermediateMat.cols(), intermediateMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(intermediateMat, intermediateBitmap)
    this.add(intermediateBitmap.toDrawable(resources))
}




