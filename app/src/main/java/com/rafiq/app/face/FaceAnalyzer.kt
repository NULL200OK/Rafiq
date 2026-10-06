package com.rafiq.app.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object FaceAnalyzer {

    suspend fun decode(context: Context, uri: Uri, maxDim: Int = 1200): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    ?: return@withContext null
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }

                val raw = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@withContext null

                val rotation = resolver.openInputStream(uri)?.use { stream ->
                    when (ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f

                val upright = if (rotation != 0f) {
                    val m = Matrix().apply { postRotate(rotation) }
                    Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                } else raw

                scaleTo(upright, maxDim)
            }.getOrNull()
        }

    suspend fun prepare(src: Bitmap): Pair<Bitmap, FaceData> = withContext(Dispatchers.IO) {
        val face = detect(src).firstOrNull()
            ?: return@withContext scaleTo(centerSquare(src), 800) to FaceData.NONE

        val bb = face.boundingBox
        val side = (max(bb.width(), bb.height()) * 1.9f).toInt().coerceIn(300, 2000)
        val cx = bb.exactCenterX()
        val cy = bb.exactCenterY() - bb.height() * 0.08f

        val left = (cx - side / 2f).coerceIn(0f, (src.width - side).coerceAtLeast(0f))
        val top = (cy - side / 2f).coerceIn(0f, (src.height - side).coerceAtLeast(0f))
        val cw = min(side, src.width - left.toInt()).toInt().coerceAtLeast(1)
        val ch = min(side, src.height - top.toInt()).toInt().coerceAtLeast(1)

        fun nx(x: Float) = ((x - left) / cw).coerceIn(0f, 1f)
        fun ny(y: Float) = ((y - top) / ch).coerceIn(0f, 1f)

        val mL = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mR = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mT = face.getLandmark(FaceLandmark.MOUTH_TOP)?.position
        val mB = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        val eL = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val eR = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        val data = if (mL != null && mR != null && eL != null && eR != null) {
            val mouthCx = (mL.x + mR.x) / 2f
            val mouthCy = if (mT != null && mB != null) (mT.y + mB.y) / 2f else (mL.y + mR.y) / 2f
            val halfW = dist(mL.x, mL.y, mR.x, mR.y) / 2f
            val mouthH = if (mT != null && mB != null) abs(mB.y - mT.y) else halfW * 0.5f
            val eyeDist = dist(eL.x, eL.y, eR.x, eR.y)

            FaceData(
                hasFace = true,
                mouthCx = nx(mouthCx), mouthCy = ny(mouthCy),
                mouthW = (halfW / cw).coerceIn(0.02f, 0.4f),
                mouthH = (mouthH / ch).coerceIn(0.005f, 0.2f),
                leftEyeX = nx(eL.x), leftEyeY = ny(eL.y),
                rightEyeX = nx(eR.x), rightEyeY = ny(eR.y),
                eyeR = ((eyeDist * 0.16f) / cw).coerceIn(0.02f, 0.15f)
            )
        } else FaceData.NONE

        val cropped = Bitmap.createBitmap(src, left.toInt(), top.toInt(), cw, ch)
        scaleTo(cropped, 800) to data
    }

    private suspend fun detect(bmp: Bitmap): List<Face> =
        suspendCancellableCoroutine { cont ->
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
            val detector = FaceDetection.getClient(options)
            detector.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { faces -> cont.resume(faces); detector.close() }
                .addOnFailureListener { cont.resume(emptyList()); detector.close() }
        }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1; val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    private fun scaleTo(bmp: Bitmap, maxDim: Int): Bitmap {
        val biggest = max(bmp.width, bmp.height)
        if (biggest <= maxDim) return bmp
        val ratio = maxDim.toFloat() / biggest
        return Bitmap.createScaledBitmap(
            bmp, (bmp.width * ratio).toInt().coerceAtLeast(1),
            (bmp.height * ratio).toInt().coerceAtLeast(1), true
        )
    }

    private fun centerSquare(bmp: Bitmap): Bitmap {
        val side = min(bmp.width, bmp.height)
        return Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
    }
}