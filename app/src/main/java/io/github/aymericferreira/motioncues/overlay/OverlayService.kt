package io.github.aymericferreira.motioncues.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.LifecycleService
import io.github.aymericferreira.motioncues.MainActivity
import io.github.aymericferreira.motioncues.R
import io.github.aymericferreira.motioncues.motion.MotionEngine
import io.github.aymericferreira.motioncues.motion.VehicleDetector
import io.github.aymericferreira.motioncues.settings.Settings
import io.github.aymericferreira.motioncues.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Foreground service that owns the WindowManager overlay and the motion sensors.
 *
 * It runs in one of two states once armed:
 *  - **active**: the overlay window is added and the dots animate.
 *  - **monitoring** (auto-detect on): no window; the [VehicleDetector] adds/removes the window
 *    in-process as a vehicle starts/stops moving. Doing this in-process avoids the Android 12+
 *    background foreground-service-start restriction — the service is only ever *started* from the
 *    foreground (app button or Quick Settings tile).
 */
class OverlayService : LifecycleService(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private lateinit var sensorManager: SensorManager
    private lateinit var store: SettingsStore

    private val engine = MotionEngine()
    private val detector = VehicleDetector()
    private var overlayView: OverlayView? = null

    private var settings: Settings = Settings()
    private var overlayShown = false

    private var lastLinearTs = 0L
    private var lastAccelTs = 0L
    private var hasLinearSensor = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        store = SettingsStore(applicationContext)
        createNotificationChannel()

        lifecycleScope.launch {
            store.settings.collect { newSettings ->
                val autoChanged = newSettings.autoDetect != settings.autoDetect
                settings = newSettings
                overlayView?.applySettings(newSettings)
                // If auto-detect was just turned off, make sure the overlay is shown while armed.
                if (autoChanged && !newSettings.autoDetect) {
                    showOverlay()
                } else if (autoChanged && newSettings.autoDetect) {
                    detector.reset()
                    if (!detector.inVehicle) hideOverlay()
                }
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopArmed()
                return START_NOT_STICKY
            }
        }
        // ACTION_START (or restart): go foreground promptly to satisfy the FGS time limit.
        startForegroundCompat()
        _running.value = true
        registerSensors()
        engine.reset()
        detector.reset()
        // Manual start with auto-detect off shows the overlay immediately; with auto-detect on we
        // stay in monitoring mode until the detector fires.
        if (!settings.autoDetect) showOverlay() else hideOverlay()
        return START_STICKY
    }

    // ---- Sensors ----

    private fun registerSensors() {
        val linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        hasLinearSensor = linear != null
        linear?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val dt = deltaSeconds(event.timestamp, lastLinearTs).also { lastLinearTs = event.timestamp }
                if (dt > 0f) {
                    val drift = engine.update(event.values[0], event.values[1], event.values[2], settings.sensitivity, dt)
                    overlayView?.setDrift(drift)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val dt = deltaSeconds(event.timestamp, lastAccelTs).also { lastAccelTs = event.timestamp }
                if (dt <= 0f) return
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                if (!hasLinearSensor) {
                    overlayView?.setDrift(engine.updateFromAccelerometer(x, y, z, settings.sensitivity, dt))
                }
                if (settings.autoDetect) {
                    val inVehicle = detector.addSample(sqrt(x * x + y * y + z * z), dt)
                    if (inVehicle && !overlayShown) {
                        showOverlay(); updateNotification()
                    } else if (!inVehicle && overlayShown) {
                        hideOverlay(); updateNotification()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun deltaSeconds(now: Long, last: Long): Float {
        if (last == 0L) return 0f
        return ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
    }

    // ---- Overlay window ----

    private fun showOverlay() {
        if (overlayShown) return
        val view = overlayView ?: OverlayView(this).also {
            it.applySettings(settings)
            overlayView = it
        }
        windowManager.addView(view, overlayLayoutParams())
        view.start()
        overlayShown = true
    }

    private fun hideOverlay() {
        if (!overlayShown) return
        overlayView?.let {
            it.stop()
            runCatching { windowManager.removeView(it) }
        }
        overlayShown = false
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    )

    // ---- Lifecycle / foreground ----

    private fun stopArmed() {
        _running.value = false
        hideOverlay()
        runCatching { sensorManager.unregisterListener(this) }
        stopForegroundCompat()
        stopSelf()
    }

    override fun onDestroy() {
        _running.value = false
        hideOverlay()
        runCatching { sensorManager.unregisterListener(this) }
        overlayView = null
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        // The "specialUse" FGS type (and its permission) only exist from Android 14 (API 34).
        // On older versions, declaring a type isn't required, so use the 2-arg form.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun updateNotification() {
        if (!_running.value) return
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_motion_cues)
        .setContentTitle(getString(R.string.notif_title))
        .setContentText(
            getString(
                if (overlayShown) R.string.notif_text_active else R.string.notif_text_monitoring
            )
        )
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .addAction(
            R.drawable.ic_motion_cues,
            getString(R.string.notif_stop),
            PendingIntent.getService(
                this, 1,
                Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.notif_channel_desc) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "motion_cues_overlay"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "io.github.aymericferreira.motioncues.START"
        const val ACTION_STOP = "io.github.aymericferreira.motioncues.STOP"

        private val _running = MutableStateFlow(false)

        /** Observable running state for the UI and the Quick Settings tile. */
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OverlayService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, OverlayService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
