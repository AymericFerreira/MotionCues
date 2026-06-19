package io.github.aymericferreira.motioncues.motion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleDetectorTest {

    private val g = 9.81f

    /** Feed alternating +/- deviation around gravity to simulate driving vibration. */
    private fun VehicleDetector.driveFor(seconds: Float, amplitude: Float, dt: Float = 0.05f) {
        val steps = (seconds / dt).toInt()
        repeat(steps) { i ->
            val mag = g + if (i % 2 == 0) amplitude else -amplitude
            addSample(mag, dt)
        }
    }

    private fun VehicleDetector.stillFor(seconds: Float, dt: Float = 0.05f) {
        val steps = (seconds / dt).toInt()
        repeat(steps) { addSample(g, dt) }
    }

    @Test fun `sustained vibration enters in-vehicle after the dwell`() {
        val d = VehicleDetector()
        d.driveFor(seconds = 2f, amplitude = 1.0f)
        assertFalse("should not enter before the dwell elapses", d.inVehicle)
        d.driveFor(seconds = 5f, amplitude = 1.0f)
        assertTrue("should be in-vehicle after sustained vibration", d.inVehicle)
    }

    @Test fun `prolonged stillness leaves in-vehicle after the exit dwell`() {
        val d = VehicleDetector()
        d.driveFor(seconds = 8f, amplitude = 1.0f)
        assertTrue(d.inVehicle)
        d.stillFor(seconds = 6f)
        assertTrue("should not exit before the longer exit dwell", d.inVehicle)
        d.stillFor(seconds = 14f)
        assertFalse("should leave in-vehicle after prolonged stillness", d.inVehicle)
    }

    @Test fun `energy held in the hysteresis band never toggles`() {
        // Amplitude ~0.4 sits between exit (0.25) and enter (0.6) thresholds.
        val d = VehicleDetector()
        d.driveFor(seconds = 60f, amplitude = 0.4f)
        assertFalse("hysteresis band must not trigger entry", d.inVehicle)
    }

    @Test fun `a brief bump does not trigger entry`() {
        val d = VehicleDetector()
        d.driveFor(seconds = 1.0f, amplitude = 1.5f) // short jolt
        d.stillFor(seconds = 3f)
        assertFalse(d.inVehicle)
    }
}
