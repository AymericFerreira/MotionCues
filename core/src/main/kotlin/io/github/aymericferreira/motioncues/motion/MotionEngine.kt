package io.github.aymericferreira.motioncues.motion

import io.github.aymericferreira.motioncues.model.Vec2

/**
 * Turns vehicle acceleration into a screen-space drift vector for the dot field. Pure Kotlin so it
 * can be unit-tested with synthetic sensor sequences (no Android `SensorManager`).
 *
 * Output drift is in **screen fractions per second** (resolution-independent); the renderer
 * multiplies by the screen's reference size.
 *
 * ## Sign convention (pinned by tests)
 * Inputs are vehicle proper-acceleration: `longitudinal` positive = accelerating forward,
 * `lateral` positive = pushed to the right. Dots flow *opposite* to motion so peripheral vision
 * sees "scenery" moving the way the inner ear feels it:
 *  - accelerate forward  -> dots drift **down**  (drift.y > 0, screen +y is down)
 *  - brake               -> dots drift **up**
 *  - accelerate rightward -> dots drift **left**  (drift.x < 0)
 *
 * ## v1 orientation simplification
 * We assume the phone is held roughly upright (portrait reading position): device +X is lateral
 * (right) and device -Z is the travel direction. Instead of full orientation tracking we use an
 * adaptive bias (a slow-moving mean that is subtracted) so a constant tilt or steady cruising speed
 * decays the drift back to zero. Full orientation handling is a later version.
 */
class MotionEngine(private val config: Config = Config()) {

    data class Config(
        /** Smoothing time constant for the acceleration signal, seconds. */
        val accelTau: Float = 0.20f,
        /** Time constant for the adaptive bias (cancels steady tilt / constant speed), seconds. */
        val biasTau: Float = 3.0f,
        /** Gravity low-pass time constant for the accelerometer fallback, seconds. */
        val gravityTau: Float = 1.0f,
        /** Drift gain (screen-fraction/s per m/s²) at sensitivity 0. */
        val minGain: Float = 0.020f,
        /** Drift gain at sensitivity 1. */
        val maxGain: Float = 0.120f,
    )

    // Smoothed lateral/longitudinal acceleration (m/s²).
    private var smoothLat = 0f
    private var smoothLong = 0f

    // Adaptive bias removed from the raw axes.
    private var biasLat = 0f
    private var biasLong = 0f

    // Gravity estimate for the accelerometer fallback path.
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private var gravitySeeded = false

    var currentDrift: Vec2 = Vec2.ZERO
        private set

    /** Reset all internal state (call when the overlay (re)activates). */
    fun reset() {
        smoothLat = 0f; smoothLong = 0f
        biasLat = 0f; biasLong = 0f
        gravitySeeded = false
        currentDrift = Vec2.ZERO
    }

    /**
     * Pure mapping from vehicle acceleration to drift velocity. Exposed for direct testing of the
     * sign convention and gain scaling.
     */
    fun mapAccelToDrift(lateral: Float, longitudinal: Float, sensitivity: Float): Vec2 {
        val gain = config.minGain + sensitivity.coerceIn(0f, 1f) * (config.maxGain - config.minGain)
        // See sign convention above: forward -> +y (down), right -> -x (left).
        return Vec2(x = -gain * lateral, y = gain * longitudinal)
    }

    /**
     * Feed a sample from the linear-acceleration sensor (gravity already removed), device frame.
     * Returns the updated drift.
     */
    fun update(linX: Float, linY: Float, linZ: Float, sensitivity: Float, dt: Float): Vec2 {
        val rawLat = linX
        val rawLong = -linZ // forward travel is device -Z in the upright assumption

        // Adaptive bias: slow mean of each axis, subtracted to cancel steady offsets.
        val biasAlpha = alpha(dt, config.biasTau)
        biasLat += biasAlpha * (rawLat - biasLat)
        biasLong += biasAlpha * (rawLong - biasLong)

        // Smooth the de-biased signal to remove high-frequency jitter.
        val accelAlpha = alpha(dt, config.accelTau)
        smoothLat += accelAlpha * ((rawLat - biasLat) - smoothLat)
        smoothLong += accelAlpha * ((rawLong - biasLong) - smoothLong)

        currentDrift = mapAccelToDrift(smoothLat, smoothLong, sensitivity)
        return currentDrift
    }

    /**
     * Fallback for devices without `TYPE_LINEAR_ACCELERATION`: feed raw accelerometer (includes
     * gravity). A low-pass estimates gravity, which is subtracted before [update].
     */
    fun updateFromAccelerometer(ax: Float, ay: Float, az: Float, sensitivity: Float, dt: Float): Vec2 {
        if (!gravitySeeded) {
            gravityX = ax; gravityY = ay; gravityZ = az
            gravitySeeded = true
        } else {
            val g = alpha(dt, config.gravityTau)
            gravityX += g * (ax - gravityX)
            gravityY += g * (ay - gravityY)
            gravityZ += g * (az - gravityZ)
        }
        return update(ax - gravityX, ay - gravityY, az - gravityZ, sensitivity, dt)
    }

    private companion object {
        /** Convert a time constant into a per-sample EMA coefficient for the given dt. */
        fun alpha(dt: Float, tau: Float): Float {
            if (tau <= 0f) return 1f
            val a = dt / (tau + dt)
            return a.coerceIn(0f, 1f)
        }
    }
}
