package io.github.aymericferreira.motioncues.overlay

import io.github.aymericferreira.motioncues.model.Vec2
import io.github.aymericferreira.motioncues.settings.PatternStyle
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Holds and advances the dot positions. Pure geometry in **normalized [0,1) screen space** so it is
 * resolution-independent and unit-testable; the renderer multiplies by the view's pixel size.
 *
 * Each dot eases its velocity toward the target drift (so motion is smooth and "recenters" — i.e.
 * settles to a stop — when the drift returns to zero), then integrates and wraps toroidally so the
 * field never empties. [PatternStyle.STATIC] lays the dots on a fixed lattice and ignores drift,
 * giving a stationary reference grid.
 */
class DotField(
    count: Int,
    pattern: PatternStyle,
    private val seed: Long = 1L,
    /** How quickly dot velocity eases toward the target drift, seconds. */
    private val velocityTau: Float = 0.35f,
) {
    data class Dot(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f)

    var pattern: PatternStyle = pattern
        private set

    private val _dots = ArrayList<Dot>()
    val dots: List<Dot> get() = _dots

    init {
        rebuild(count, pattern)
    }

    /** Recreate the field for a new count and/or pattern (e.g. when settings change). */
    fun rebuild(count: Int, pattern: PatternStyle) {
        this.pattern = pattern
        _dots.clear()
        val n = count.coerceAtLeast(1)
        when (pattern) {
            PatternStyle.DYNAMIC -> {
                val rng = Random(seed)
                repeat(n) { _dots.add(Dot(rng.nextFloat(), rng.nextFloat())) }
            }
            PatternStyle.STATIC -> {
                // Square-ish lattice covering the field.
                val cols = ceil(kotlin.math.sqrt(n.toFloat())).roundToInt().coerceAtLeast(1)
                val rows = ceil(n.toFloat() / cols).roundToInt().coerceAtLeast(1)
                var placed = 0
                outer@ for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (placed >= n) break@outer
                        val x = (c + 0.5f) / cols
                        val y = (r + 0.5f) / rows
                        _dots.add(Dot(x, y))
                        placed++
                    }
                }
            }
        }
    }

    /**
     * Advance the field by [dt] seconds given the current [drift] (screen fractions/second).
     * STATIC fields ignore drift and stay put.
     */
    fun update(drift: Vec2, dt: Float) {
        val target = if (pattern == PatternStyle.STATIC) Vec2.ZERO else drift
        val ease = if (velocityTau <= 0f) 1f else (dt / (velocityTau + dt)).coerceIn(0f, 1f)
        for (d in _dots) {
            d.vx += ease * (target.x - d.vx)
            d.vy += ease * (target.y - d.vy)
            d.x = wrap01(d.x + d.vx * dt)
            d.y = wrap01(d.y + d.vy * dt)
        }
    }

    private companion object {
        fun wrap01(v: Float): Float {
            var r = v % 1f
            if (r < 0f) r += 1f
            return r
        }
    }
}
