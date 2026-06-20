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
 * Behaviour depends on [PatternStyle]:
 *  - [PatternStyle.DYNAMIC]: dots scattered across the whole screen; each eases its velocity toward
 *    the drift (smooth motion that settles to a stop when drift returns to zero) and wraps toroidally.
 *  - [PatternStyle.EDGES]: dots sit on a rectangle inset from the edges (clear center) and slide
 *    *along* the border in the direction of motion — top/bottom dots track turns, side dots track
 *    acceleration/braking. This mirrors Apple's Vehicle Motion Cues.
 *  - [PatternStyle.STATIC]: a fixed lattice that ignores drift (a stationary reference grid).
 */
class DotField(
    count: Int,
    pattern: PatternStyle,
    private val seed: Long = 1L,
    /** How quickly dot velocity eases toward the target drift (DYNAMIC only), seconds. */
    private val velocityTau: Float = 0.35f,
) {
    /** [t] is the perimeter parameter [0,1) used by EDGES; unused by other patterns. */
    data class Dot(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f, var t: Float = 0f)

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
            PatternStyle.EDGES -> {
                // Evenly spaced around the inset perimeter.
                repeat(n) { i ->
                    val t = i.toFloat() / n
                    val (x, y) = edgePos(t)
                    _dots.add(Dot(x, y, t = t))
                }
            }
            PatternStyle.STATIC -> {
                // Square-ish lattice covering the field.
                val cols = ceil(kotlin.math.sqrt(n.toFloat())).roundToInt().coerceAtLeast(1)
                val rows = ceil(n.toFloat() / cols).roundToInt().coerceAtLeast(1)
                var placed = 0
                outer@ for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (placed >= n) break@outer
                        _dots.add(Dot((c + 0.5f) / cols, (r + 0.5f) / rows))
                        placed++
                    }
                }
            }
        }
    }

    /**
     * Advance the field by [dt] seconds given the current [drift] (screen fractions/second).
     */
    fun update(drift: Vec2, dt: Float) {
        when (pattern) {
            PatternStyle.STATIC -> return // fixed reference grid
            PatternStyle.EDGES -> {
                // Move each dot along the border by the drift component tangent to its edge, so the
                // physical speed along the edge matches the drift speed.
                val loopLen = 4f * (1f - 2f * EDGE_INSET)
                for (d in _dots) {
                    val (tx, ty) = edgeTangent(d.t)
                    val along = drift.x * tx + drift.y * ty
                    d.t = wrap01(d.t + (along / loopLen) * dt)
                    val (x, y) = edgePos(d.t)
                    d.x = x; d.y = y
                }
            }
            PatternStyle.DYNAMIC -> {
                val ease = if (velocityTau <= 0f) 1f else (dt / (velocityTau + dt)).coerceIn(0f, 1f)
                for (d in _dots) {
                    d.vx += ease * (drift.x - d.vx)
                    d.vy += ease * (drift.y - d.vy)
                    d.x = wrap01(d.x + d.vx * dt)
                    d.y = wrap01(d.y + d.vy * dt)
                }
            }
        }
    }

    private companion object {
        /** Inset of the edge ring from the screen border, in screen fractions. */
        const val EDGE_INSET = 0.1f

        fun wrap01(v: Float): Float {
            var r = v % 1f
            if (r < 0f) r += 1f
            return r
        }

        /** Position on the inset perimeter for parameter [t] in [0,1), walking clockwise. */
        fun edgePos(t: Float): Pair<Float, Float> {
            val m = EDGE_INSET
            val s = 1f - 2f * m
            val tt = wrap01(t) * 4f
            return when {
                tt < 1f -> (m + tt * s) to m                 // top: left -> right
                tt < 2f -> (1f - m) to (m + (tt - 1f) * s)   // right: top -> bottom
                tt < 3f -> (1f - m - (tt - 2f) * s) to (1f - m) // bottom: right -> left
                else -> m to (1f - m - (tt - 3f) * s)        // left: bottom -> top
            }
        }

        /** Unit tangent (direction of increasing [t]) on the edge containing parameter [t]. */
        fun edgeTangent(t: Float): Pair<Float, Float> {
            val tt = wrap01(t) * 4f
            return when {
                tt < 1f -> 1f to 0f
                tt < 2f -> 0f to 1f
                tt < 3f -> -1f to 0f
                else -> 0f to -1f
            }
        }
    }
}
