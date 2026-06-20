package io.github.aymericferreira.motioncues.settings

/**
 * Serializes [Settings] to/from a small, human-readable `key=value` text format for backup, export,
 * and sharing presets. Pure Kotlin (no Android or JSON dependencies) so it is fully unit-testable.
 *
 * Decoding is tolerant: blank lines and `#` comments are ignored, unknown or missing keys fall back
 * to defaults, and every value is coerced into its valid range — so a hand-edited or older file can
 * never push the overlay into a bad state.
 */
object SettingsCodec {
    const val VERSION = 1

    fun encode(s: Settings): String = buildString {
        appendLine("# Motion Cues settings")
        appendLine("version=$VERSION")
        appendLine("dotColor=${s.dotColor}")
        appendLine("dotCount=${s.dotCount}")
        appendLine("dotSizeDp=${s.dotSizeDp}")
        appendLine("dotOpacity=${s.dotOpacity}")
        appendLine("sensitivity=${s.sensitivity}")
        appendLine("backgroundDim=${s.backgroundDim}")
        appendLine("patternStyle=${s.patternStyle.name}")
        appendLine("autoDetect=${s.autoDetect}")
    }

    fun decode(text: String): Settings {
        val m = HashMap<String, String>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            m[line.substring(0, eq).trim()] = line.substring(eq + 1).trim()
        }
        val d = Settings()
        return Settings(
            dotColor = m["dotColor"]?.toIntOrNull() ?: d.dotColor,
            dotCount = (m["dotCount"]?.toIntOrNull() ?: d.dotCount).coerceIn(Settings.DOT_COUNT_RANGE),
            dotSizeDp = (m["dotSizeDp"]?.toFloatOrNull() ?: d.dotSizeDp).coerceIn(Settings.DOT_SIZE_DP_RANGE),
            dotOpacity = (m["dotOpacity"]?.toFloatOrNull() ?: d.dotOpacity).coerceIn(Settings.DOT_OPACITY_RANGE),
            sensitivity = (m["sensitivity"]?.toFloatOrNull() ?: d.sensitivity).coerceIn(Settings.SENSITIVITY_RANGE),
            backgroundDim = (m["backgroundDim"]?.toFloatOrNull() ?: d.backgroundDim).coerceIn(Settings.BACKGROUND_DIM_RANGE),
            patternStyle = PatternStyle.fromName(m["patternStyle"]),
            autoDetect = m["autoDetect"]?.toBooleanStrictOrNull() ?: d.autoDetect,
        )
    }
}
