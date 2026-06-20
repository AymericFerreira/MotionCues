package io.github.aymericferreira.motioncues.overlay

import io.github.aymericferreira.motioncues.model.Vec2
import io.github.aymericferreira.motioncues.settings.PatternStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DotFieldTest {

    @Test fun `dynamic field has the requested number of dots inside bounds`() {
        val field = DotField(count = 40, pattern = PatternStyle.DYNAMIC)
        assertEquals(40, field.dots.size)
        field.dots.forEach {
            assertTrue(it.x in 0f..1f && it.y in 0f..1f)
        }
    }

    @Test fun `dots wrap toroidally instead of leaving the screen`() {
        val field = DotField(count = 1, pattern = PatternStyle.DYNAMIC)
        field.dots[0].x = 0.9f
        field.dots[0].y = 0.9f
        // Strong drift over large steps would push a non-wrapping dot off-screen.
        repeat(20) { field.update(Vec2(1.5f, 1.5f), dt = 0.1f) }
        val d = field.dots[0]
        assertTrue("x=${d.x} out of range", d.x >= 0f && d.x < 1f)
        assertTrue("y=${d.y} out of range", d.y >= 0f && d.y < 1f)
    }

    @Test fun `dots settle to a stop when drift returns to zero`() {
        val field = DotField(count = 5, pattern = PatternStyle.DYNAMIC)
        repeat(50) { field.update(Vec2(0.4f, -0.3f), dt = 0.02f) } // build up motion
        repeat(200) { field.update(Vec2.ZERO, dt = 0.02f) }        // 4s of no drift
        field.dots.forEach {
            assertTrue("vx=${it.vx} should have decayed", abs(it.vx) < 0.01f)
            assertTrue("vy=${it.vy} should have decayed", abs(it.vy) < 0.01f)
        }
    }

    @Test fun `static pattern lays a lattice and ignores drift`() {
        val field = DotField(count = 9, pattern = PatternStyle.STATIC)
        assertEquals(9, field.dots.size)
        val before = field.dots.map { it.x to it.y }
        // A 3x3 lattice: first dot centered in the first cell.
        assertEquals(0.5f / 3f, field.dots[0].x, 1e-5f)
        assertEquals(0.5f / 3f, field.dots[0].y, 1e-5f)

        repeat(30) { field.update(Vec2(2f, 2f), dt = 0.1f) }
        val after = field.dots.map { it.x to it.y }
        assertEquals("static dots must not move", before, after)
    }

    @Test fun `rebuild switches pattern and count`() {
        val field = DotField(count = 10, pattern = PatternStyle.DYNAMIC)
        field.rebuild(count = 16, pattern = PatternStyle.STATIC)
        assertEquals(16, field.dots.size)
        assertEquals(PatternStyle.STATIC, field.pattern)
    }

    // ---- Edges pattern ----

    private val inset = 0.1f

    private fun onBorder(x: Float, y: Float, tol: Float = 1e-3f): Boolean =
        abs(x - inset) < tol || abs(x - (1f - inset)) < tol ||
            abs(y - inset) < tol || abs(y - (1f - inset)) < tol

    @Test fun `edges pattern places all dots on the border with a clear center`() {
        val field = DotField(count = 8, pattern = PatternStyle.EDGES)
        assertEquals(8, field.dots.size)
        field.dots.forEach { d ->
            assertTrue("dot $d should sit on the edge ring", onBorder(d.x, d.y))
            val inCenter = d.x > 0.3f && d.x < 0.7f && d.y > 0.3f && d.y < 0.7f
            assertFalse("dot $d must not be in the clear center", inCenter)
        }
    }

    @Test fun `edges dots travel along the border under drift and stay on it`() {
        val field = DotField(count = 4, pattern = PatternStyle.EDGES)
        val before = field.dots.map { it.x to it.y }
        repeat(60) { field.update(Vec2(0.3f, 0.2f), dt = 0.05f) }
        assertNotEquals("edge dots should move under drift", before, field.dots.map { it.x to it.y })
        field.dots.forEach { d ->
            assertTrue("dot $d left the border ring", onBorder(d.x, d.y))
        }
    }
}
