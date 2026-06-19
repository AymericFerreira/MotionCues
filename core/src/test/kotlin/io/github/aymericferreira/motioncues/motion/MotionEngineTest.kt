package io.github.aymericferreira.motioncues.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionEngineTest {

    private val engine = MotionEngine()

    // ---- Sign convention (pure mapping) ----

    @Test fun `forward acceleration drifts dots down`() {
        val d = engine.mapAccelToDrift(lateral = 0f, longitudinal = 3f, sensitivity = 1f)
        assertTrue("expected downward drift (+y), got $d", d.y > 0f)
        assertEquals(0f, d.x, 1e-6f)
    }

    @Test fun `braking drifts dots up`() {
        val d = engine.mapAccelToDrift(lateral = 0f, longitudinal = -3f, sensitivity = 1f)
        assertTrue("expected upward drift (-y), got $d", d.y < 0f)
    }

    @Test fun `rightward acceleration drifts dots left`() {
        val d = engine.mapAccelToDrift(lateral = 3f, longitudinal = 0f, sensitivity = 1f)
        assertTrue("expected leftward drift (-x), got $d", d.x < 0f)
        assertEquals(0f, d.y, 1e-6f)
    }

    @Test fun `higher sensitivity yields larger drift`() {
        val low = engine.mapAccelToDrift(0f, 3f, sensitivity = 0.1f).magnitude
        val high = engine.mapAccelToDrift(0f, 3f, sensitivity = 0.9f).magnitude
        assertTrue("high($high) should exceed low($low)", high > low)
    }

    // ---- Full pipeline (filtering + decay) ----

    @Test fun `sustained forward accel produces downward drift then idle decays to near zero`() {
        engine.reset()
        val dt = 0.02f
        // 1.5s of forward acceleration: linear-accel sensor, forward travel is device -Z.
        repeat(75) { engine.update(linX = 0f, linY = 0f, linZ = -3f, sensitivity = 1f, dt = dt) }
        val peak = engine.currentDrift
        assertTrue("expected downward drift during accel, got $peak", peak.y > 0.05f)

        // 12s of stillness -> drift should decay back toward zero.
        repeat(600) { engine.update(0f, 0f, 0f, sensitivity = 1f, dt = dt) }
        val idle = engine.currentDrift
        assertTrue("idle drift ${idle.magnitude} should be small", idle.magnitude < 0.02f)
        assertTrue("idle drift should be far below peak", idle.magnitude < 0.2f * peak.magnitude)
    }

    @Test fun `accelerometer fallback removes gravity so a parked phone barely drifts`() {
        engine.reset()
        val dt = 0.02f
        // Constant 1g along device Y (upright phone), no motion.
        repeat(200) { engine.updateFromAccelerometer(ax = 0f, ay = 9.81f, az = 0f, sensitivity = 1f, dt = dt) }
        assertTrue("parked drift ${engine.currentDrift.magnitude} should be ~0", engine.currentDrift.magnitude < 0.01f)
    }

    @Test fun `accelerometer fallback still reacts to a real acceleration transient`() {
        engine.reset()
        val dt = 0.02f
        repeat(100) { engine.updateFromAccelerometer(0f, 9.81f, 0f, 1f, dt) } // settle gravity
        // Add a forward (device -Z) acceleration on top of gravity.
        repeat(40) { engine.updateFromAccelerometer(0f, 9.81f, -3f, 1f, dt) }
        assertTrue("expected downward drift transient, got ${engine.currentDrift}", engine.currentDrift.y > 0.02f)
    }

    @Test fun `zero input keeps drift at zero`() {
        engine.reset()
        repeat(50) { engine.update(0f, 0f, 0f, 0.5f, 0.02f) }
        assertEquals(0f, engine.currentDrift.magnitude, 1e-4f)
    }
}
