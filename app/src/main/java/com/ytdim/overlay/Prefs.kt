package com.ytdim.overlay

import android.content.Context
import android.content.SharedPreferences

/** All user settings, stored in one SharedPreferences file. */
object Prefs {
    const val FILE = "settings"

    const val ENABLED = "enabled"
    const val COLOR = "color"            // RGB, no alpha (e.g. 0x000000)
    const val OPACITY = "opacity"        // 0..100 percent
    const val GAP_TOP = "gap_top"        // per-mille of screen height (0..1000)
    const val GAP_HEIGHT = "gap_height"  // per-mille of screen height (0..1000)
    const val HIDE_LANDSCAPE = "hide_landscape"
    const val PACKAGES = "packages"      // comma/space/newline separated
    const val PREVIEW = "preview"        // show overlay everywhere while tuning

    const val DEFAULT_PACKAGES =
        "com.google.android.youtube\napp.morphe.android.youtube\napp.revanced.android.youtube"

    fun get(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun enabled(p: SharedPreferences) = p.getBoolean(ENABLED, false)
    fun color(p: SharedPreferences) = p.getInt(COLOR, 0x000000)
    fun opacity(p: SharedPreferences) = p.getInt(OPACITY, 85)
    // Defaults match a typical 20:9 phone in portrait: status bar ~4.8%,
    // the player below it ~25.8% of screen height.
    fun gapTop(p: SharedPreferences) = p.getInt(GAP_TOP, 48)
    fun gapHeight(p: SharedPreferences) = p.getInt(GAP_HEIGHT, 258)
    fun hideLandscape(p: SharedPreferences) = p.getBoolean(HIDE_LANDSCAPE, true)
    fun preview(p: SharedPreferences) = p.getBoolean(PREVIEW, false)

    fun packages(p: SharedPreferences): Set<String> =
        (p.getString(PACKAGES, DEFAULT_PACKAGES) ?: DEFAULT_PACKAGES)
            .split(',', ' ', '\n', '\t', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    /** ARGB colour combining the chosen colour and opacity. */
    fun argb(p: SharedPreferences): Int {
        val a = (opacity(p).coerceIn(0, 100) * 255 + 50) / 100
        return (a shl 24) or (color(p) and 0xFFFFFF)
    }
}
