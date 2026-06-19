package io.github.aymericferreira.motioncues.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val PRESETS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFFD740,
    0xFF69F0AE, 0xFF40C4FF, 0xFF7C4DFF, 0xFFFF80AB,
).map { it.toInt() }

/**
 * Lightweight in-house color picker: a row of preset swatches plus R/G/B sliders for a custom
 * color. Avoids any third-party dependency so the FOSS tree stays minimal. The color is kept
 * opaque (alpha forced to 0xFF).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: Int,
    onColorChange: (Int) -> Unit,
) {
    val color = Color(selectedColor or 0xFF000000.toInt())
    val emit: (Color) -> Unit = { onColorChange(it.copy(alpha = 1f).toArgb()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { preset ->
                val opaque = preset or 0xFF000000.toInt()
                Swatch(
                    color = Color(opaque),
                    selected = opaque == (selectedColor or 0xFF000000.toInt()),
                    onClick = { onColorChange(opaque) },
                )
            }
        }
        ChannelSlider("R", color.red) { emit(color.copy(red = it)) }
        ChannelSlider("G", color.green) { emit(color.copy(green = it)) }
        ChannelSlider("B", color.blue) { emit(color.copy(blue = it)) }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ChannelSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "$label ${(value * 255).roundToInt()}",
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f, modifier = Modifier.weight(1f))
    }
}
