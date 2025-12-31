package org.example.app.qibla.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.example.app.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class QiblaCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = ContextCompat.getColor(context, R.color.divider_soft)
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.ocean_text)
        alpha = 90
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ocean_text)
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }

    private val qiblaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.ocean_secondary)
    }

    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.ocean_primary)
        alpha = 220
    }

    private var qiblaBearingDeg: Double = 0.0
    private var deviceAzimuthDeg: Double = 0.0

    private var renderRelativeDeg: Float = 0f
    private var animator: ValueAnimator? = null

    fun setQiblaBearingDegrees(bearing: Double) {
        qiblaBearingDeg = bearing
        invalidateSmooth()
    }

    fun setDeviceAzimuthDegrees(azimuth: Double) {
        deviceAzimuthDeg = azimuth
        invalidateSmooth()
    }

    private fun invalidateSmooth() {
        val target = normalize((qiblaBearingDeg - deviceAzimuthDeg).toFloat())

        // If the change is tiny, avoid restarting animations; just draw.
        val delta = shortestDelta(renderRelativeDeg, target)
        if (abs(delta) < 0.6f) {
            renderRelativeDeg = target
            invalidate()
            return
        }

        animateTo(target)
    }

    private fun animateTo(target: Float) {
        val current = renderRelativeDeg
        val delta = shortestDelta(current, target)
        val end = current + delta

        animator?.cancel()
        animator = ValueAnimator.ofFloat(current, end).apply {
            duration = 140
            addUpdateListener {
                renderRelativeDeg = normalize((it.animatedValue as Float))
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        val radius = min(w, h) * 0.38f

        // Outer ring
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // Tick marks
        for (deg in 0 until 360 step 30) {
            val angle = Math.toRadians(deg.toDouble())
            val inner = radius * 0.88f
            val outer = radius * 1.0f
            val x1 = cx + inner * sin(angle).toFloat()
            val y1 = cy - inner * cos(angle).toFloat()
            val x2 = cx + outer * sin(angle).toFloat()
            val y2 = cy - outer * cos(angle).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // N label at top (for orientation)
        canvas.drawText("N", cx, cy - radius - 20f, textPaint)

        // Draw Qibla arrow relative to device heading
        drawArrow(canvas, cx, cy, radius * 0.9f, renderRelativeDeg, qiblaPaint)

        // Draw small "north" arrow (always top) as reference
        drawArrow(canvas, cx, cy, radius * 0.55f, 0f, northPaint)
    }

    private fun drawArrow(canvas: Canvas, cx: Float, cy: Float, length: Float, deg: Float, paint: Paint) {
        val angle = Math.toRadians(deg.toDouble())
        val tipX = cx + length * sin(angle).toFloat()
        val tipY = cy - length * cos(angle).toFloat()

        val base = length * 0.18f
        val leftAngle = Math.toRadians((deg - 18).toDouble())
        val rightAngle = Math.toRadians((deg + 18).toDouble())

        val leftX = cx + (length - base) * sin(leftAngle).toFloat()
        val leftY = cy - (length - base) * cos(leftAngle).toFloat()

        val rightX = cx + (length - base) * sin(rightAngle).toFloat()
        val rightY = cy - (length - base) * cos(rightAngle).toFloat()

        val path = Path().apply {
            moveTo(tipX, tipY)
            lineTo(leftX, leftY)
            lineTo(rightX, rightY)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun normalize(v: Float): Float {
        var x = v % 360f
        if (x < 0) x += 360f
        return x
    }

    private fun shortestDelta(from: Float, to: Float): Float {
        var delta = (to - from) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }
}
