package io.github.aymericferreira.motioncues.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** True if the app may draw the overlay window over other apps. */
fun Context.hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

/** True if the app may post the foreground-service notification (always true below Android 13). */
fun Context.hasNotificationPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
