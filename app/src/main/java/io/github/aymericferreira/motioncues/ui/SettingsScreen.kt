package io.github.aymericferreira.motioncues.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aymericferreira.motioncues.R
import io.github.aymericferreira.motioncues.settings.PatternStyle
import io.github.aymericferreira.motioncues.settings.Settings
import kotlin.math.roundToInt

/** Callbacks for persisting each setting; implemented in [io.github.aymericferreira.motioncues.MainActivity]. */
interface SettingsActions {
    fun setColor(value: Int)
    fun setCount(value: Int)
    fun setSize(value: Float)
    fun setSensitivity(value: Float)
    fun setDimming(value: Float)
    fun setPattern(value: PatternStyle)
    fun setAutoDetect(value: Boolean)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    running: Boolean,
    hasOverlay: Boolean,
    onRequestOverlay: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onAbout: () -> Unit,
    actions: SettingsActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                actions = {
                    TextButton(onClick = onAbout) {
                        Text(stringResource(R.string.settings_about))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!hasOverlay) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_needs_overlay))
                        Button(onClick = onRequestOverlay) {
                            Text(stringResource(R.string.onboard_perm_overlay_button))
                        }
                    }
                }
            }

            Button(
                onClick = if (running) onStop else onStart,
                enabled = hasOverlay || running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (running) R.string.settings_stop else R.string.settings_start))
            }

            SwitchRow(
                title = stringResource(R.string.settings_auto_detect),
                subtitle = stringResource(R.string.settings_auto_detect_desc),
                checked = settings.autoDetect,
                onCheckedChange = actions::setAutoDetect,
            )

            SectionTitle(stringResource(R.string.settings_pattern))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.patternStyle == PatternStyle.DYNAMIC,
                    onClick = { actions.setPattern(PatternStyle.DYNAMIC) },
                    label = { Text(stringResource(R.string.settings_pattern_dynamic)) },
                )
                FilterChip(
                    selected = settings.patternStyle == PatternStyle.STATIC,
                    onClick = { actions.setPattern(PatternStyle.STATIC) },
                    label = { Text(stringResource(R.string.settings_pattern_static)) },
                )
            }

            SectionTitle(stringResource(R.string.settings_color))
            ColorPicker(
                selectedColor = settings.dotColor,
                onColorChange = actions::setColor,
            )

            LabeledSlider(
                label = stringResource(R.string.settings_count),
                value = settings.dotCount.toFloat(),
                valueText = settings.dotCount.toString(),
                range = Settings.DOT_COUNT_RANGE.first.toFloat()..Settings.DOT_COUNT_RANGE.last.toFloat(),
                onValueChange = { actions.setCount(it.roundToInt()) },
            )

            LabeledSlider(
                label = stringResource(R.string.settings_size),
                value = settings.dotSizeDp,
                valueText = "${settings.dotSizeDp.roundToInt()} dp",
                range = Settings.DOT_SIZE_DP_RANGE,
                onValueChange = actions::setSize,
            )

            LabeledSlider(
                label = stringResource(R.string.settings_sensitivity),
                value = settings.sensitivity,
                valueText = "${(settings.sensitivity * 100).roundToInt()}%",
                range = Settings.SENSITIVITY_RANGE,
                onValueChange = actions::setSensitivity,
            )

            LabeledSlider(
                label = stringResource(R.string.settings_dimming),
                value = settings.backgroundDim,
                valueText = "${(settings.backgroundDim * 100).roundToInt()}%",
                range = Settings.BACKGROUND_DIM_RANGE,
                onValueChange = actions::setDimming,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(valueText, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}
