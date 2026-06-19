package io.github.aymericferreira.motioncues.model

import kotlin.math.hypot

/** Immutable 2D vector used for drift/velocity math. Pure Kotlin, no Android types. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)

    val magnitude: Float get() = hypot(x, y)

    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}
