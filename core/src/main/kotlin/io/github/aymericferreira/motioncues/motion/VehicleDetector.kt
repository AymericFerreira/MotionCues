package io.github.aymericferreira.motioncues.motion

import kotlin.math.abs

/**
 * Heuristic "are we in a moving vehicle?" detector using only the accelerometer magnitude — no Play
 * Services / ActivityRecognition, so it stays FOSS. Pure Kotlin and unit-testable.
 *
 * Idea: a moving car produces sustained low-level vibration/acceleration energy above a parked
 * phone's near-zero baseline. We track a slow mean (baseline incl. gravity) and the smoothed
 * absolute deviation from it ("energy"). Entering/leaving the in-vehicle state uses **hysteresis**
 * (enter threshold > exit threshold) plus **dwell timers**, so brief bumps or stops at a light do
 * not toggle the overlay.
 *
 * This deliberately accepts some false positives (e.g. brisk walking) for v1; a GPS-speed upgrade is
 * out of scope.
 */
class VehicleDetector(private val config: Config = Config()) {

    data class Config(
        val baselineTau: Float = 2.0f,
        val energyTau: Float = 0.5f,
        /** Energy above this for [enterDwellSeconds] flips to in-vehicle (m/s²). */
        val enterThreshold: Float = 0.6f,
        /** Energy below this for [exitDwellSeconds] flips out (m/s²); < enter for hysteresis. */
        val exitThreshold: Float = 0.25f,
        val enterDwellSeconds: Float = 4.0f,
        val exitDwellSeconds: Float = 12.0f,
    )

    private var baseline = 0f
    private var energy = 0f
    private var seeded = false
    private var aboveTimer = 0f
    private var belowTimer = 0f

    var inVehicle: Boolean = false
        private set

    fun reset() {
        baseline = 0f; energy = 0f; seeded = false
        aboveTimer = 0f; belowTimer = 0f
        inVehicle = false
    }

    /**
     * Add one accelerometer-magnitude sample. Returns the (possibly updated) [inVehicle] state.
     * @param magnitude `sqrt(x²+y²+z²)` of the accelerometer reading (m/s²).
     */
    fun addSample(magnitude: Float, dt: Float): Boolean {
        if (!seeded) {
            baseline = magnitude
            seeded = true
        } else {
            baseline += alpha(dt, config.baselineTau) * (magnitude - baseline)
        }
        val deviation = abs(magnitude - baseline)
        energy += alpha(dt, config.energyTau) * (deviation - energy)

        if (energy >= config.enterThreshold) {
            aboveTimer += dt; belowTimer = 0f
        } else if (energy <= config.exitThreshold) {
            belowTimer += dt; aboveTimer = 0f
        } else {
            // In the hysteresis band: hold both timers steady.
            aboveTimer = 0f; belowTimer = 0f
        }

        if (!inVehicle && aboveTimer >= config.enterDwellSeconds) {
            inVehicle = true; belowTimer = 0f
        } else if (inVehicle && belowTimer >= config.exitDwellSeconds) {
            inVehicle = false; aboveTimer = 0f
        }
        return inVehicle
    }

    private companion object {
        fun alpha(dt: Float, tau: Float): Float {
            if (tau <= 0f) return 1f
            return (dt / (tau + dt)).coerceIn(0f, 1f)
        }
    }
}
