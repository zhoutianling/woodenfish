package com.zero.woodenfish

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class TapEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Ripple(val startedAt: Long)

    private data class Particle(
        val angle: Float,
        val distance: Float,
        val size: Float,
        val startedAt: Long
    )

    private val ripples = mutableListOf<Ripple>()
    private val particles = mutableListOf<Particle>()
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(1.4f)
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    fun emitTap() {
        val now = AnimationUtils.currentAnimationTimeMillis()
        ripples += Ripple(now)
        repeat(9) {
            particles += Particle(
                angle = Random.nextFloat() * (PI.toFloat() * 2f),
                distance = dp(Random.nextInt(64, 132).toFloat()),
                size = dp(Random.nextInt(2, 5).toFloat()),
                startedAt = now
            )
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = AnimationUtils.currentAnimationTimeMillis()
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(width, height) * 0.28f
        var hasActiveEffect = false

        ripples.removeAll { now - it.startedAt > RIPPLE_DURATION_MS }
        particles.removeAll { now - it.startedAt > PARTICLE_DURATION_MS }

        ripples.forEach { ripple ->
            val progress = ((now - ripple.startedAt).toFloat() / RIPPLE_DURATION_MS).coerceIn(0f, 1f)
            val radius = baseRadius + progress * min(width, height) * 0.18f
            ripplePaint.alpha = (100 * (1f - progress)).toInt().coerceIn(0, 100)
            ripplePaint.strokeWidth = dp(1.2f + progress * 1.6f)
            canvas.drawCircle(cx, cy, radius, ripplePaint)
            hasActiveEffect = progress < 1f
        }

        particles.forEach { particle ->
            val progress = ((now - particle.startedAt).toFloat() / PARTICLE_DURATION_MS).coerceIn(0f, 1f)
            val easeOut = 1f - (1f - progress) * (1f - progress)
            val x = cx + cos(particle.angle) * particle.distance * easeOut
            val y = cy + sin(particle.angle) * particle.distance * easeOut
            particlePaint.alpha = (150 * (1f - progress)).toInt().coerceIn(0, 150)
            canvas.drawCircle(x, y, particle.size * (1f - progress * 0.35f), particlePaint)
            hasActiveEffect = progress < 1f
        }

        if (hasActiveEffect) {
            postInvalidateOnAnimation()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        const val RIPPLE_DURATION_MS = 700L
        const val PARTICLE_DURATION_MS = 620L
    }
}
