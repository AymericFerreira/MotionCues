package io.github.aymericferreira.motioncues.tile

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.aymericferreira.motioncues.MainActivity
import io.github.aymericferreira.motioncues.overlay.OverlayService

/**
 * Quick Settings tile that toggles the overlay. A tile click is a valid foreground trigger for
 * starting the foreground service. If the overlay permission isn't granted yet, it opens the app
 * instead so the user can grant it.
 */
class MotionCuesTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (OverlayService.running.value) {
            OverlayService.stop(this)
        } else if (Settings.canDrawOverlays(this)) {
            OverlayService.start(this)
        } else {
            openApp()
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (OverlayService.running.value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = android.app.PendingIntent.getActivity(
                this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
