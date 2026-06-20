package io.github.aymericferreira.motioncues.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Choreographer
import android.view.View
import io.github.aymericferreira.motioncues.model.Vec2
import io.github.aymericferreira.motioncues.settings.Settings

/**
 * Full-screen transparent view that paints the dimming scrim and the dot field every frame. Driven
 * by [Choreographer] so it advances in step with the display refresh. The geometry lives in the
 * pure-Kotlin [DotField]; this view only scales normalized positions to pixels and draws.
 */
@SuppressLint("ViewConstructor")
class OverlayView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint()

    private var dotField = DotField(Settings.DEFAULT_DOT_COUNT, Settings().patternStyle)
    private var dotRadiusPx = Settings.DEFAULT_DOT_SIZE_DP * density
    private var scrimAlpha = (Settings.DEFAULT_BACKGROUND_DIM * 255).toInt()

    @Volatile private var drift: Vec2 = Vec2.ZERO

    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var lastFrameNs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val dt = if (lastFrameNs == 0L) 0f
            else ((frameTimeNanos - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.1f)
            lastFrameNs = frameTimeNanos
            dotField.update(drift, dt)
            invalidate()
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNs = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }

    /** Live-apply settings. Rebuilds the field only when count/pattern actually change. */
    fun applySettings(s: Settings) {
        dotPaint.color = s.dotColor
        // Opacity slider overrides the color's alpha channel.
        dotPaint.alpha = (s.dotOpacity.coerceIn(0f, 1f) * 255).toInt()
        dotRadiusPx = s.dotSizeDp * density
        scrimAlpha = (s.backgroundDim.coerceIn(0f, 1f) * 255).toInt()
        scrimPaint.color = Color.argb(scrimAlpha, 0, 0, 0)
        if (s.dotCount != dotField.dots.size || s.patternStyle != dotField.pattern) {
            dotField.rebuild(s.dotCount, s.patternStyle)
        }
        invalidate()
    }

    fun setDrift(value: Vec2) {
        drift = value
    }

    override fun onDraw(canvas: Canvas) {
        if (scrimAlpha > 0) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }
        val w = width.toFloat()
        val h = height.toFloat()
        val dots = dotField.dots
        for (i in dots.indices) {
            val d = dots[i]
            canvas.drawCircle(d.x * w, d.y * h, dotRadiusPx, dotPaint)
        }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}
