package com.rafiq.app.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.rafiq.app.R
import java.io.File
import java.io.FileOutputStream

object FaceStore {

    private const val IMG = "rafiq_avatar.jpg"
    private const val META = "rafiq_avatar.json"

    fun save(context: Context, bmp: Bitmap, data: FaceData) {
        runCatching {
            File(context.filesDir, META).writeText(data.toJson())
            FileOutputStream(File(context.filesDir, IMG)).use {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
        }
    }

    fun load(context: Context): Pair<Bitmap, FaceData>? {
        val img = File(context.filesDir, IMG)
        if (!img.exists()) return null
        val bmp = BitmapFactory.decodeFile(img.absolutePath) ?: return null
        val meta = File(context.filesDir, META)
        return bmp to FaceData.fromJson(meta.takeIf { it.exists() }?.readText())
    }

    fun clear(context: Context) {
        File(context.filesDir, IMG).delete()
        File(context.filesDir, META).delete()
    }

    fun defaultFace(context: Context): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher)!!
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bmp))
        return bmp
    }
}