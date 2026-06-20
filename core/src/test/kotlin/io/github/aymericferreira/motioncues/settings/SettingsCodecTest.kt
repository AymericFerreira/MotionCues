package io.github.aymericferreira.motioncues.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCodecTest {

    @Test fun `round-trip preserves all in-range fields`() {
        val original = Settings(
            dotColor = 0xFFFF8800.toInt(),
            dotCount = 12,
            dotSizeDp = 10f,
            dotOpacity = 0.6f,
            sensitivity = 0.7f,
            backgroundDim = 0.3f,
            patternStyle = PatternStyle.DYNAMIC,
            autoDetect = true,
        )
        assertEquals(original, SettingsCodec.decode(SettingsCodec.encode(original)))
    }

    @Test fun `decode coerces out-of-range values into their ranges`() {
        val text = """
            dotCount=999
            dotSizeDp=100.0
            dotOpacity=5.0
            backgroundDim=-1.0
        """.trimIndent()
        val s = SettingsCodec.decode(text)
        assertEquals(Settings.DOT_COUNT_RANGE.last, s.dotCount)
        assertEquals(Settings.DOT_SIZE_DP_RANGE.endInclusive, s.dotSizeDp, 1e-6f)
        assertEquals(Settings.DOT_OPACITY_RANGE.endInclusive, s.dotOpacity, 1e-6f)
        assertEquals(Settings.BACKGROUND_DIM_RANGE.start, s.backgroundDim, 1e-6f)
    }

    @Test fun `decode tolerates comments, blanks, junk and missing keys`() {
        val text = "# a preset\n\nnonsense line\ndotCount=5\npatternStyle=STATIC\n"
        val s = SettingsCodec.decode(text)
        val d = Settings()
        assertEquals(5, s.dotCount)
        assertEquals(PatternStyle.STATIC, s.patternStyle)
        // Untouched keys fall back to defaults.
        assertEquals(d.dotColor, s.dotColor)
        assertEquals(d.sensitivity, s.sensitivity, 1e-6f)
    }

    @Test fun `decode of an unknown pattern name falls back to the default`() {
        val s = SettingsCodec.decode("patternStyle=WOBBLE")
        assertEquals(Settings().patternStyle, s.patternStyle)
    }
}
