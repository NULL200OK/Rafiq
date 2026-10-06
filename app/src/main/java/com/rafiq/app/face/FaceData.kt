package com.rafiq.app.face

import org.json.JSONObject

data class FaceData(
    val hasFace: Boolean = false,
    val mouthCx: Float = 0.5f, val mouthCy: Float = 0.62f,
    val mouthW: Float = 0.12f,
    val mouthH: Float = 0.05f,
    val leftEyeX: Float = 0.38f, val leftEyeY: Float = 0.38f,
    val rightEyeX: Float = 0.62f, val rightEyeY: Float = 0.38f,
    val eyeR: Float = 0.05f
) {
    fun toJson(): String = JSONObject().apply {
        put("hf", hasFace)
        put("mx", mouthCx.toDouble()); put("my", mouthCy.toDouble())
        put("mw", mouthW.toDouble()); put("mh", mouthH.toDouble())
        put("lx", leftEyeX.toDouble()); put("ly", leftEyeY.toDouble())
        put("rx", rightEyeX.toDouble()); put("ry", rightEyeY.toDouble())
        put("er", eyeR.toDouble())
    }.toString()

    companion object {
        val NONE = FaceData()

        fun fromJson(json: String?): FaceData {
            if (json.isNullOrBlank()) return NONE
            return runCatching {
                val o = JSONObject(json)
                FaceData(
                    hasFace = o.optBoolean("hf", false),
                    mouthCx = o.optDouble("mx", 0.5).toFloat(),
                    mouthCy = o.optDouble("my", 0.62).toFloat(),
                    mouthW = o.optDouble("mw", 0.12).toFloat(),
                    mouthH = o.optDouble("mh", 0.05).toFloat(),
                    leftEyeX = o.optDouble("lx", 0.38).toFloat(),
                    leftEyeY = o.optDouble("ly", 0.38).toFloat(),
                    rightEyeX = o.optDouble("rx", 0.62).toFloat(),
                    rightEyeY = o.optDouble("ry", 0.38).toFloat(),
                    eyeR = o.optDouble("er", 0.05).toFloat()
                )
            }.getOrDefault(NONE)
        }
    }
}