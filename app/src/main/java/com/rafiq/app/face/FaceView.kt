package com.rafiq.app.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class FaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var hasFace = false

    private val cols = 22
    private val rows = 24
    private val n = (cols + 1) * (rows + 1)
    private val verts = FloatArray(n * 2)
    private var baseX = FloatArray(n)
    private var baseY = FloatArray(n)
    private var mInf = FloatArray(n)
    private var widen = FloatArray(n)
    private var curveInf = FloatArray(n)
    private var eInfL = FloatArray(n)
    private var eInfR = FloatArray(n)

    private var mcx = 0f; private var mcy = 0f
    private var openPx = 0f
    private var sigM = 0f
    private var lEx = 0f; private var lEy = 0f
    private var rEx = 0f; private var rEy = 0f
    private var eyeR = 0f
    private var bw = 0f; private var bh = 0f

    private var emotion = Emotion.CALM
    private var curCurve = 0f
    private var curEye = 1f
    private var curTilt = 0f
    private var curEnergy = 1f

    private var speaking = false
    private var listening = false
    private var mouthOpen = 0f
    private var pulse = 0f
    private var blink = 0f
    private var blinkStart = -1L
    private var nextBlink = 0L
    private var nextFlip = 0L
    private var flipTarget = 0.3f
    private var lastFrame = 0L
    private var running = false
    private var glowShaderColor = 0

    private val meshPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
    private val cavityPaint = Paint().apply { style = Paint.Style.FILL; color = Color.rgb(34, 18, 20) }
    private val glintPaint = Paint().apply { style = Paint.Style.FILL; color = Color.WHITE; isAntiAlias = true }
    private val glowPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

    fun setFace(bmp: Bitmap, data: FaceData) {
        bitmap = bmp
        hasFace = data.hasFace
        bw = bmp.width.toFloat()
        bh = bmp.height.toFloat()

        var i = 0
        for (r in 0..rows) for (c in 0..cols) {
            baseX[i] = c * bw / cols
            baseY[i] = r * bh / rows
            i++
        }

        if (hasFace) {
            mcx = data.mouthCx * bw; mcy = data.mouthCy * bh
            openPx = max(data.mouthH * bh * 2.0f, bw * 0.055f)
            sigM = max(data.mouthW * bw * 1.6f, bw * 0.03f)
            lEx = data.leftEyeX * bw; lEy = data.leftEyeY * bh
            rEx = data.rightEyeX * bw; rEy = data.rightEyeY * bh
            eyeR = max(data.eyeR * bw, bw * 0.015f)

            val sM2 = 2f * sigM * sigM
            val sE = eyeR * 1.7f
            val sEx2 = 2f * sE * sE
            val sEy2 = 2f * (sE * 0.7f) * (sE * 0.7f)
            for (j in 0 until n) {
                val dx = baseX[j] - mcx; val dy = baseY[j] - mcy
                val mi = exp(-(dx * dx + dy * dy) / sM2)
                mInf[j] = mi
                widen[j] = mi * (dx / sigM).coerceIn(-1f, 1f)
                curveInf[j] = mi * min(abs(dx) / sigM, 1f)
                val ldx = baseX[j] - lEx; val ldy = baseY[j] - lEy
                eInfL[j] = exp(-(ldx * ldx / sEx2 + ldy * ldy / sEy2))
                val rdx = baseX[j] - rEx; val rdy = baseY[j] - rEy
                eInfR[j] = exp(-(rdx * rdx / sEx2 + rdy * rdy / sEy2))
            }
        }
        glowShaderColor = 0
        invalidate()
    }

    fun setSpeaking(value: Boolean) { speaking = value }
    fun setListening(value: Boolean) { listening = value }
    fun pulseMouth() { pulse = 1f }
    fun setEmotion(value: Emotion) { emotion = value }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            step(frameTimeNanos / 1_000_000L)
            if (running) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        super.onDetachedFromWindow()
    }

    private fun step(now: Long) {
        if (lastFrame == 0L) { lastFrame = now; return }
        val dt = (now - lastFrame).coerceAtLeast(1L) / 1000f
        lastFrame = now

        val lerp = min(1f, dt * 2.5f)
        curCurve += (emotion.mouthCurve - curCurve) * lerp
        curEye += (emotion.eyeScale - curEye) * lerp
        curTilt += (emotion.tilt - curTilt) * lerp
        curEnergy += (emotion.energy - curEnergy) * lerp

        var target = 0f
        if (speaking) {
            if (now > nextFlip) {
                flipTarget = if (Random.nextFloat() < 0.18f) 0.08f
                             else (0.3f + Random.nextFloat() * 0.7f) * curEnergy
                flipTarget = flipTarget.coerceIn(0.05f, 1.1f)
                nextFlip = now + 70L + Random.nextLong(120L)
            }
            target = flipTarget
        }
        pulse *= exp(-dt * 9f)
        if (pulse > 0.02f) target = max(target, pulse)
        val k = if (target > mouthOpen) 24f else 12f
        mouthOpen += (target - mouthOpen) * min(1f, dt * k)

        if (blinkStart < 0 && now > nextBlink) {
            blinkStart = now
            nextBlink = now + 2000L + Random.nextLong(4000L)
        }
        if (blinkStart >= 0) {
            val p = (now - blinkStart) / 150f
            if (p >= 1f) {
                blinkStart = -1L
                blink = 0f
                if (Random.nextFloat() < 0.15f) nextBlink = now + 180L
            } else {
                blink = sin(p * Math.PI).toFloat()
            }
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap ?: return
        val vw = width.toFloat(); val vh = height.toFloat()
        if (vw < 10f || vh < 10f) return

        val pad = 4f
        val scale = min((vw - pad * 2f) / bw, (vh - pad * 2f) / bh)
        val t = lastFrame / 1000f

        canvas.save()
        canvas.translate(vw / 2f, vh / 2f)

        val amp = (3f + (if (speaking) 2f else 0f) + (if (listening) 3f else 0f)) *
                (0.85f + 0.3f * curEnergy)
        val bob = sin(t * 1.7f) * amp
        if (!hasFace) canvas.rotate(sin(t * 0.9f) * 1.6f)
        canvas.rotate(curTilt)
        val breath = if (hasFace) 1f else 1f + 0.012f * sin(t * 2.3f)
        canvas.scale(scale * breath, scale * breath)
        canvas.translate(-bw / 2f, -bh / 2f + bob / scale)

        ensureGlow()
        glowPaint.alpha = (185 + 55 * sin(t * 2.1f)).toInt().coerceIn(0, 255)
        canvas.drawCircle(bw / 2f, bh / 2f, max(bw, bh) * 0.72f, glowPaint)

        if (hasFace) {
            updateVerts()
            canvas.drawBitmapMesh(bmp, cols, rows, verts, 0, null, meshPaint)

            if (mouthOpen > 0.06f) {
                cavityPaint.alpha = (mouthOpen * 150).toInt()
                val rx = max(sigM * 0.95f, bw * 0.03f) * (0.7f + 0.3f * mouthOpen)
                val ry = max(openPx * mouthOpen * 0.8f, 2f)
                canvas.drawOval(RectF(mcx - rx, mcy - ry * 0.35f, mcx + rx, mcy + ry), cavityPaint)
            }

            if (blink < 0.6f) {
                glintPaint.alpha = ((1f - blink) * 80).toInt()
                val gr = eyeR * 0.16f
                canvas.drawCircle(lEx + eyeR * 0.28f, lEy - eyeR * 0.3f, gr, glintPaint)
                canvas.drawCircle(rEx + eyeR * 0.28f, rEy - eyeR * 0.3f, gr, glintPaint)
            }
        } else {
            canvas.drawBitmap(bmp, 0f, 0f, meshPaint)
        }

        canvas.restore()
    }

    private fun ensureGlow() {
        val c = emotion.glow
        if (glowShaderColor == c && glowPaint.shader != null) return
        glowShaderColor = c
        val translucent = (0x59 shl 24) or (c and 0x00FFFFFF)
        val transparent = c and 0x00FFFFFF
        glowPaint.shader = RadialGradient(
            bw / 2f, bh / 2f, max(bw, bh) * 0.72f,
            translucent, transparent, Shader.TileMode.CLAMP
        )
    }

    private fun updateVerts() {
        val up = openPx * mouthOpen
        val curvePx = bw * 0.014f * curCurve
        val eyeDelta = curEye - 1f
        val blinkK = blink * 0.88f
        var vi = 0
        for (i in 0 until n) {
            var x = baseX[i]
            var y = baseY[i]

            val mi = mInf[i]
            if (mi > 0.004f) {
                y += if (y < mcy) -up * 0.35f * mi else up * mi
                x += up * 0.12f * widen[i]
                y -= curvePx * curveInf[i]
            }
            if (abs(eyeDelta) > 0.004f) {
                if (eInfL[i] > 0.004f) y = lEy + (y - lEy) * (1f + eyeDelta * eInfL[i])
                if (eInfR[i] > 0.004f) y = rEy + (y - rEy) * (1f + eyeDelta * eInfR[i])
            }
            if (blinkK > 0.01f) {
                if (eInfL[i] > 0.004f) y = lEy + (y - lEy) * (1f - blinkK * eInfL[i])
                if (eInfR[i] > 0.004f) y = rEy + (y - rEy) * (1f - blinkK * eInfR[i])
            }

            verts[vi++] = x
            verts[vi++] = y
        }
    }
}
