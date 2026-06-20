package io.github.aymericferreira.motioncues

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aymericferreira.motioncues.R
import io.github.aymericferreira.motioncues.overlay.OverlayService
import io.github.aymericferreira.motioncues.settings.PatternStyle
import io.github.aymericferreira.motioncues.settings.Settings
import io.github.aymericferreira.motioncues.settings.SettingsCodec
import io.github.aymericferreira.motioncues.settings.SettingsStore
import io.github.aymericferreira.motioncues.ui.AboutScreen
import io.github.aymericferreira.motioncues.ui.OnboardingScreen
import io.github.aymericferreira.motioncues.ui.SettingsActions
import io.github.aymericferreira.motioncues.ui.SettingsScreen
import io.github.aymericferreira.motioncues.ui.theme.MotionCuesTheme
import io.github.aymericferreira.motioncues.util.hasNotificationPermission
import io.github.aymericferreira.motioncues.util.hasOverlayPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotionCuesTheme {
                MotionCuesRoot()
            }
        }
    }
}

private enum class Screen { ONBOARDING, SETTINGS, ABOUT }

@Composable
private fun MotionCuesRoot() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()

    val settings by store.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val running by OverlayService.running.collectAsStateWithLifecycle()

    var hasOverlay by remember { mutableStateOf(context.hasOverlayPermission()) }
    var hasNotif by remember { mutableStateOf(context.hasNotificationPermission()) }

    // Permissions are granted in system UI; re-check whenever we return to the foreground.
    LifecycleResumeEffect(Unit) {
        hasOverlay = context.hasOverlayPermission()
        hasNotif = context.hasNotificationPermission()
        onPauseOrDispose { }
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasOverlay = context.hasOverlayPermission() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotif = granted }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(SettingsCodec.encode(settings).toByteArray())
                }
            }.isSuccess
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.toast_settings_exported else R.string.toast_settings_error),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: error("empty")
                store.setAll(SettingsCodec.decode(text))
            }.isSuccess
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.toast_settings_imported else R.string.toast_settings_error),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val requestOverlay: () -> Unit = {
        overlayLauncher.launch(
            Intent(
                AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        )
    }
    val requestNotif: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var screen by rememberSaveable {
        mutableStateOf(if (context.hasOverlayPermission()) Screen.SETTINGS else Screen.ONBOARDING)
    }

    when (screen) {
        Screen.ONBOARDING -> OnboardingScreen(
            hasOverlay = hasOverlay,
            hasNotif = hasNotif,
            onRequestOverlay = requestOverlay,
            onRequestNotif = requestNotif,
            onContinue = { screen = Screen.SETTINGS },
        )

        Screen.SETTINGS -> SettingsScreen(
            settings = settings,
            running = running,
            hasOverlay = hasOverlay,
            onRequestOverlay = requestOverlay,
            onStart = {
                if (hasNotif.not()) requestNotif()
                OverlayService.start(context)
            },
            onStop = { OverlayService.stop(context) },
            onAbout = { screen = Screen.ABOUT },
            onExport = { exportLauncher.launch("motion-cues-settings.txt") },
            onImport = { importLauncher.launch(arrayOf("text/plain", "application/octet-stream")) },
            actions = object : SettingsActions {
                override fun setColor(value: Int) { scope.launch { store.setDotColor(value) } }
                override fun setCount(value: Int) { scope.launch { store.setDotCount(value) } }
                override fun setSize(value: Float) { scope.launch { store.setDotSizeDp(value) } }
                override fun setOpacity(value: Float) { scope.launch { store.setDotOpacity(value) } }
                override fun setSensitivity(value: Float) { scope.launch { store.setSensitivity(value) } }
                override fun setDimming(value: Float) { scope.launch { store.setBackgroundDim(value) } }
                override fun setPattern(value: PatternStyle) { scope.launch { store.setPatternStyle(value) } }
                override fun setAutoDetect(value: Boolean) { scope.launch { store.setAutoDetect(value) } }
            },
        )

        Screen.ABOUT -> AboutScreen(onBack = { screen = Screen.SETTINGS })
    }
}
