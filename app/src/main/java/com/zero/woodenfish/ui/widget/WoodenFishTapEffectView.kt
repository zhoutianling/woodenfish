package com.zero.woodenfish.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.zero.woodenfish.R
import com.zero.woodenfish.ui.extension.dpToPx
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class WoodenFishTapEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val rippleStartedAt = LongArray(MAX_RIPPLES)
    private val particleStartedAt = LongArray(MAX_PARTICLES)
    private val particleAngles = FloatArray(MAX_PARTICLES)
    private val particleDistances = FloatArray(MAX_PARTICLES)
    private val particleSizes = FloatArray(MAX_PARTICLES)
    private var rippleCount = 0
    private var particleCount = 0
    private var centerX = 0f
    private var centerY = 0f
    private var baseRippleRadius = 0f
    private var rippleTravelDistance = 0f
    private val minRippleStrokeWidth = context.dpToPx(MIN_RIPPLE_STROKE_DP)
    private val rippleStrokeWidthGrowth = context.dpToPx(RIPPLE_STROKE_GROWTH_DP)

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.fish_symbol_tint)
        style = Paint.Style.STROKE
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.fish_symbol_tint)
        style = Paint.Style.FILL
    }

    fun emitTap() {
        val now = AnimationUtils.currentAnimationTimeMillis()
        appendRipple(now)
        repeat(PARTICLES_PER_TAP) {
            appendParticle(
                startedAt = now,
                angle = Random.nextFloat() * FULL_CIRCLE_RADIANS,
                distance = context.dpToPx(Random.nextInt(MIN_PARTICLE_DISTANCE_DP, MAX_PARTICLE_DISTANCE_DP).toFloat()),
                size = context.dpToPx(Random.nextInt(MIN_PARTICLE_SIZE_DP, MAX_PARTICLE_SIZE_DP).toFloat())
            )
        }
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val minSize = min(width, height).toFloat()
        centerX = width / 2f
        centerY = height / 2f
        baseRippleRadius = minSize * BASE_RIPPLE_RADIUS_RATIO
        rippleTravelDistance = minSize * RIPPLE_TRAVEL_RATIO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = AnimationUtils.currentAnimationTimeMillis()
        var hasActiveEffect = false
        compactExpiredEffects(now)

        for (index in 0 until rippleCount) {
            val progress = progress(now, rippleStartedAt[index], RIPPLE_DURATION_MS)
            val radius = baseRippleRadius + progress * rippleTravelDistance
            ripplePaint.alpha = (MAX_RIPPLE_ALPHA * (1f - progress)).toInt().coerceIn(0, MAX_RIPPLE_ALPHA)
            ripplePaint.strokeWidth = minRippleStrokeWidth + progress * rippleStrokeWidthGrowth
            canvas.drawCircle(centerX, centerY, radius, ripplePaint)
            hasActiveEffect = hasActiveEffect || progress < 1f
        }

        for (index in 0 until particleCount) {
            val progress = progress(now, particleStartedAt[index], PARTICLE_DURATION_MS)
            val easedProgress = easeOutQuad(progress)
            val x = centerX + cos(particleAngles[index]) * particleDistances[index] * easedProgress
            val y = centerY + sin(particleAngles[index]) * particleDistances[index] * easedProgress
            particlePaint.alpha = (MAX_PARTICLE_ALPHA * (1f - progress)).toInt().coerceIn(0, MAX_PARTICLE_ALPHA)
            canvas.drawCircle(x, y, particleSizes[index] * (1f - progress * PARTICLE_SIZE_SHRINK_RATIO), particlePaint)
            hasActiveEffect = hasActiveEffect || progress < 1f
        }

        if (hasActiveEffect) {
            postInvalidateOnAnimation()
        }
    }

    private fun appendRipple(startedAt: Long) {
        if (rippleCount == MAX_RIPPLES) {
            System.arraycopy(rippleStartedAt, 1, rippleStartedAt, 0, MAX_RIPPLES - 1)
            rippleCount--
        }
        rippleStartedAt[rippleCount++] = startedAt
    }

    private fun appendParticle(startedAt: Long, angle: Float, distance: Float, size: Float) {
        if (particleCount == MAX_PARTICLES) {
            val retainedCount = MAX_PARTICLES - PARTICLES_PER_TAP
            System.arraycopy(particleStartedAt, PARTICLES_PER_TAP, particleStartedAt, 0, retainedCount)
            System.arraycopy(particleAngles, PARTICLES_PER_TAP, particleAngles, 0, retainedCount)
            System.arraycopy(particleDistances, PARTICLES_PER_TAP, particleDistances, 0, retainedCount)
            System.arraycopy(particleSizes, PARTICLES_PER_TAP, particleSizes, 0, retainedCount)
            particleCount = retainedCount
        }
        particleStartedAt[particleCount] = startedAt
        particleAngles[particleCount] = angle
        particleDistances[particleCount] = distance
        particleSizes[particleCount] = size
        particleCount++
    }

    private fun compactExpiredEffects(now: Long) {
        var rippleWriteIndex = 0
        for (rippleReadIndex in 0 until rippleCount) {
            val startedAt = rippleStartedAt[rippleReadIndex]
            if (now - startedAt <= RIPPLE_DURATION_MS) {
                rippleStartedAt[rippleWriteIndex++] = startedAt
            }
        }
        rippleCount = rippleWriteIndex

        var writeIndex = 0
        for (readIndex in 0 until particleCount) {
            if (now - particleStartedAt[readIndex] <= PARTICLE_DURATION_MS) {
                if (writeIndex != readIndex) {
                    particleStartedAt[writeIndex] = particleStartedAt[readIndex]
                    particleAngles[writeIndex] = particleAngles[readIndex]
                    particleDistances[writeIndex] = particleDistances[readIndex]
                    particleSizes[writeIndex] = particleSizes[readIndex]
                }
                writeIndex++
            }
        }
        particleCount = writeIndex
    }

    private fun progress(now: Long, startedAt: Long, durationMs: Long): Float {
        return ((now - startedAt).toFloat() / durationMs).coerceIn(0f, 1f)
    }

    private fun easeOutQuad(progress: Float): Float = 1f - (1f - progress) * (1f - progress)

    private companion object {
        const val MAX_RIPPLES = 6
        const val MAX_PARTICLES = 54
        const val PARTICLES_PER_TAP = 9
        const val RIPPLE_DURATION_MS = 700L
        const val PARTICLE_DURATION_MS = 620L
        const val FULL_CIRCLE_RADIANS = PI.toFloat() * 2f
        const val BASE_RIPPLE_RADIUS_RATIO = 0.28f
        const val RIPPLE_TRAVEL_RATIO = 0.18f
        const val MAX_RIPPLE_ALPHA = 100
        const val MAX_PARTICLE_ALPHA = 150
        const val MIN_RIPPLE_STROKE_DP = 1.2f
        const val RIPPLE_STROKE_GROWTH_DP = 1.6f
        const val MIN_PARTICLE_DISTANCE_DP = 64
        const val MAX_PARTICLE_DISTANCE_DP = 132
        const val MIN_PARTICLE_SIZE_DP = 2
        const val MAX_PARTICLE_SIZE_DP = 5
        const val PARTICLE_SIZE_SHRINK_RATIO = 0.35f
    }
}
