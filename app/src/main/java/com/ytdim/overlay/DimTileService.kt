package com.ytdim.overlay

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: tap to turn the overlay on/off. */
class DimTileService : TileService() {

    companion object {
        fun requestUpdate(ctx: Context) {
            try {
                requestListeningState(ctx, ComponentName(ctx, DimTileService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    override fun onClick() {
        super.onClick()
        if (OverlayService.running == null) {
            // The accessibility service isn't on yet, so send the user there.
            launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val p = Prefs.get(this)
        p.edit().putBoolean(Prefs.ENABLED, !Prefs.enabled(p)).apply()
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        val serviceOn = OverlayService.running != null
        val enabled = Prefs.enabled(Prefs.get(this))
        tile.state = if (serviceOn && enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !serviceOn -> "Tap to set up"
                enabled -> "On"
                else -> "Off"
            }
        }
        tile.updateTile()
    }

    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
