package io.github.aymericferreira.motioncues.settings

/** Visual pattern of the dot field. */
enum class PatternStyle {
    /** Dots scattered across the whole screen, drifting with vehicle motion. */
    DYNAMIC,

    /**
     * Dots arranged around the screen edges with a clear center, drifting along the border with
     * motion. Mirrors Apple's Vehicle Motion Cues look and keeps the reading area unobstructed.
     */
    EDGES,

    /** A fixed reference lattice that does not move (a static visual anchor). */
    STATIC;

    companion object {
        fun fromName(name: String?): PatternStyle =
            entries.firstOrNull { it.name == name } ?: EDGES
    }
}

/**
 * Immutable snapshot of all user-configurable settings. Persisted by the Android `SettingsStore`
 * (DataStore) but defined here as plain Kotlin so the [io.github.aymericferreira.motioncues.motion]
 * and overlay logic can stay testable without Android.
 *
 * @param dotColor packed ARGB color (the alpha channel is overridden by [dotOpacity]).
 * @param dotCount number of dots in the field.
 * @param dotSizeDp dot radius in dp.
 * @param dotOpacity 0..1 opacity of the dots themselves.
 * @param sensitivity 0..1 normalized; scales how strongly motion moves the dots.
 * @param backgroundDim 0..1 opacity of the dimming scrim behind the dots.
 */
data class Settings(
    val dotColor: Int = DEFAULT_DOT_COLOR,
    val dotCount: Int = DEFAULT_DOT_COUNT,
    val dotSizeDp: Float = DEFAULT_DOT_SIZE_DP,
    val dotOpacity: Float = DEFAULT_DOT_OPACITY,
    val sensitivity: Float = DEFAULT_SENSITIVITY,
    val backgroundDim: Float = DEFAULT_BACKGROUND_DIM,
    val patternStyle: PatternStyle = PatternStyle.EDGES,
    val autoDetect: Boolean = DEFAULT_AUTO_DETECT,
) {
    companion object {
        const val DEFAULT_DOT_COLOR: Int = 0xFFFFFFFF.toInt()
        const val DEFAULT_DOT_COUNT: Int = 8
        const val DEFAULT_DOT_SIZE_DP: Float = 8f
        const val DEFAULT_DOT_OPACITY: Float = 0.9f
        const val DEFAULT_SENSITIVITY: Float = 0.5f
        const val DEFAULT_BACKGROUND_DIM: Float = 0.15f
        const val DEFAULT_AUTO_DETECT: Boolean = false

        val DOT_COUNT_RANGE: IntRange = 2..20
        val DOT_SIZE_DP_RANGE: ClosedFloatingPointRange<Float> = 3f..24f
        val DOT_OPACITY_RANGE: ClosedFloatingPointRange<Float> = 0.1f..1f
        val SENSITIVITY_RANGE: ClosedFloatingPointRange<Float> = 0f..1f
        val BACKGROUND_DIM_RANGE: ClosedFloatingPointRange<Float> = 0f..0.8f
    }
}
