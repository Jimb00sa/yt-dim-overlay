package com.ytdim.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Accessibility service that draws a see-through dimming layer over the
 * screen while one of the chosen apps (YouTube / Morphe) is in front,
 * leaving a clear band where the video player is.
 *
 * It uses TYPE_ACCESSIBILITY_OVERLAY rather than a normal "draw over other
 * apps" window: accessibility overlays are trusted by Android, so taps and
 * scrolls pass straight through at any opacity (normal overlays above 80%
 * opacity block touches on Android 12+). It also needs no extra permission.
 */
class OverlayService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        @Volatile
        var running: OverlayService? = null
            private set
    }

    private lateinit var prefs: SharedPreferences
    private var overlay: DimView? = null
    private var foregroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = this
        prefs = Prefs.get(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        addOverlay()
        refresh()
        DimTileService.requestUpdate(this)
    }

    override fun onDestroy() {
        running = null
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(this)
        removeOverlay()
        DimTileService.requestUpdate(this)
        super.onDestroy()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        running = null
        removeOverlay()
        DimTileService.requestUpdate(this)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val pkg = detectForegroundApp(event)
                if (pkg != null && pkg != foregroundPackage) {
                    foregroundPackage = pkg
                    refresh()
                }
            }
        }
    }

    /** Package of the app window the user is currently looking at. */
    private fun detectForegroundApp(event: AccessibilityEvent): String? {
        try {
            val appWindows = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            val active = appWindows.firstOrNull { it.isActive }
                ?: appWindows.firstOrNull { it.isFocused }
            val pkg = active?.root?.packageName?.toString()
            if (pkg != null) return pkg
        } catch (_: Exception) {
            // Fall through to the event's own package.
        }
        // Fallback: ignore the system UI and keyboards, which raise events
        // without the user actually leaving the app.
        val pkg = event.packageName?.toString() ?: return null
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return null
        if (pkg == "com.android.systemui" || pkg.contains("inputmethod") || pkg.contains("keyboard")) return null
        return pkg
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refresh()
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) {
        if (key == Prefs.ENABLED) DimTileService.requestUpdate(this)
        refresh()
    }

    private fun addOverlay() {
        if (overlay != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = DimView(this)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lp.fitInsetsTypes = 0
        }
        lp.title = "YT Dim Overlay"
        view.visibility = View.GONE
        wm.addView(view, lp)
        overlay = view
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        overlay = null
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
        } catch (_: Exception) {
        }
    }

    private fun refresh() {
        val view = overlay ?: return
        if (!::prefs.isInitialized) return
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val inTargetApp = foregroundPackage != null && foregroundPackage in Prefs.packages(prefs)
        val show = Prefs.enabled(prefs) &&
            (Prefs.preview(prefs) || inTargetApp) &&
            !(landscape && Prefs.hideLandscape(prefs))

        view.argb = Prefs.argb(prefs)
        view.gapTop = Prefs.gapTop(prefs) / 1000f
        view.gapHeight = Prefs.gapHeight(prefs) / 1000f
        view.invalidate()
        view.visibility = if (show) View.VISIBLE else View.GONE
    }

    /** Paints the dim colour everywhere except the clear video band. */
    private class DimView(ctx: Context) : View(ctx) {
        var argb: Int = 0xD9000000.toInt()
        var gapTop: Float = 0f
        var gapHeight: Float = 0f
        private val paint = Paint()

        override fun onDraw(canvas: Canvas) {
            paint.color = argb
            val w = width.toFloat()
            val h = height.toFloat()
            val top = (gapTop * h).coerceIn(0f, h)
            val bottom = ((gapTop + gapHeight) * h).coerceIn(top, h)
            if (top > 0f) canvas.drawRect(0f, 0f, w, top, paint)
            if (bottom < h) canvas.drawRect(0f, bottom, w, h, paint)
        }
    }
}
