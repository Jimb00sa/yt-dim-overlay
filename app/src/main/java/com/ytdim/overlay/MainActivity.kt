package com.ytdim.overlay

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Settings screen, built in code to keep the project dependency-free. */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var serviceStatus: TextView
    private lateinit var serviceButton: Button
    private lateinit var enabledSwitch: Switch
    private lateinit var hexInput: EditText
    private lateinit var swatch: View
    private var updatingHex = false

    private val presets = listOf(
        "Black" to 0x000000,
        "Charcoal" to 0x202020,
        "Deep red" to 0x3A0000,
        "Amber" to 0x3A2000,
        "Navy" to 0x000A2A,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.get(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }

        root.addView(title("YT Dim Overlay"))
        root.addView(body("Dims everything except the video while YouTube or Morphe is open. Taps and scrolls pass through the overlay."))

        // --- Service status ---
        root.addView(header("1. Turn on the service"))
        serviceStatus = body("")
        root.addView(serviceStatus)
        serviceButton = Button(this).apply {
            text = "Open accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        root.addView(serviceButton)
        root.addView(body("Find \"YT Dim Overlay\" in the list (sometimes under \"Installed apps\" or \"Downloaded apps\") and switch it on. If Android says it's a restricted setting, open this app's App info, tap the ⋮ menu and choose \"Allow restricted settings\", then try again."))

        // --- On/off ---
        root.addView(header("2. Overlay"))
        enabledSwitch = Switch(this).apply {
            text = "Overlay on"
            textSize = 17f
            isChecked = Prefs.enabled(prefs)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(Prefs.ENABLED, on).apply() }
        }
        root.addView(enabledSwitch)
        root.addView(body("Tip: add the \"YT Dim\" tile to Quick Settings (pull down twice, tap the pencil) to toggle it from anywhere."))

        root.addView(Switch(this).apply {
            text = "Preview on this screen while adjusting"
            isChecked = false
            prefs.edit().putBoolean(Prefs.PREVIEW, false).apply()
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(Prefs.PREVIEW, on).apply() }
        })

        // --- Colour ---
        root.addView(header("Colour"))
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((name, rgb) in presets) {
            presetRow.addView(Button(this).apply {
                text = name
                isAllCaps = false
                setOnClickListener { setColor(rgb) }
            })
        }
        root.addView(HorizontalScrollView(this).apply { addView(presetRow) })

        val hexRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        swatch = View(this)
        hexRow.addView(swatch, LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(12) })
        hexInput = EditText(this).apply {
            hint = "#000000"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (updatingHex) return
                    val txt = s?.toString()?.trim()?.removePrefix("#") ?: return
                    if (txt.length == 6) txt.toIntOrNull(16)?.let { saveColor(it) }
                }
            })
        }
        hexRow.addView(hexInput, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        root.addView(hexRow)
        root.addView(body("Type any hex colour, e.g. #1A0A00. Dark reds and ambers are easiest on night vision."))

        // --- Opacity ---
        root.addView(header("Darkness (opacity)"))
        root.addView(slider(0, 100, Prefs.opacity(prefs), { "$it%" }) {
            prefs.edit().putInt(Prefs.OPACITY, it).apply()
        })

        // --- Video gap ---
        root.addView(header("Clear area for the video"))
        root.addView(body("The band left undimmed, measured from the top of the screen. Turn on Preview, then line it up with where YouTube shows the player."))
        root.addView(body("Top edge"))
        val topSlider = slider(0, 600, Prefs.gapTop(prefs), { pct(it) }) {
            prefs.edit().putInt(Prefs.GAP_TOP, it).apply()
        }
        root.addView(topSlider)
        root.addView(body("Height"))
        val heightSlider = slider(0, 1000, Prefs.gapHeight(prefs), { pct(it) }) {
            prefs.edit().putInt(Prefs.GAP_HEIGHT, it).apply()
        }
        root.addView(heightSlider)
        root.addView(Button(this).apply {
            text = "Reset clear area"
            setOnClickListener {
                prefs.edit().remove(Prefs.GAP_TOP).remove(Prefs.GAP_HEIGHT).apply()
                recreate()
            }
        })

        root.addView(Switch(this).apply {
            text = "Hide overlay in landscape (fullscreen video)"
            isChecked = Prefs.hideLandscape(prefs)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(Prefs.HIDE_LANDSCAPE, on).apply() }
        })

        // --- Apps ---
        root.addView(header("Apps to dim"))
        root.addView(body("Package names, one per line. The overlay only shows while one of these is in front."))
        val appsInput = EditText(this).apply {
            setText(prefs.getString(Prefs.PACKAGES, Prefs.DEFAULT_PACKAGES))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 3
            textSize = 14f
            typeface = Typeface.MONOSPACE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    prefs.edit().putString(Prefs.PACKAGES, s?.toString() ?: "").apply()
                }
            })
        }
        root.addView(appsInput)

        val found = findYouTubeApps()
        root.addView(body(if (found.isEmpty()) "No YouTube-like apps detected."
            else "Detected on this phone:"))
        for ((label, pkg) in found) {
            root.addView(Button(this).apply {
                isAllCaps = false
                text = "Add $label ($pkg)"
                setOnClickListener {
                    val current = Prefs.packages(prefs)
                    if (pkg !in current) {
                        val txt = appsInput.text.toString().trimEnd()
                        appsInput.setText(if (txt.isEmpty()) pkg else "$txt\n$pkg")
                    }
                }
            })
        }

        setContentView(ScrollView(this).apply { addView(root, MATCH_PARENT, WRAP_CONTENT) })
        showColor(Prefs.color(prefs))
    }

    override fun onResume() {
        super.onResume()
        val on = OverlayService.running != null
        serviceStatus.text = if (on) "✅ Service is on." else "⚠️ Service is off — the overlay can't show until you turn it on."
        serviceButton.text = if (on) "Accessibility settings" else "Open accessibility settings"
        enabledSwitch.isChecked = Prefs.enabled(prefs)
    }

    override fun onStop() {
        // Never leave the preview stuck on over other apps.
        prefs.edit().putBoolean(Prefs.PREVIEW, false).apply()
        super.onStop()
    }

    private fun findYouTubeApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .map { it.loadLabel(packageManager).toString() to it.activityInfo.packageName }
                .filter { (label, pkg) ->
                    pkg.contains("youtube", true) || label.contains("youtube", true) ||
                        pkg.contains("morphe", true) || pkg.contains("revanced", true)
                }
                .filter { it.second != packageName }
                .distinctBy { it.second }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun setColor(rgb: Int) {
        saveColor(rgb)
        showColor(rgb)
    }

    private fun saveColor(rgb: Int) {
        prefs.edit().putInt(Prefs.COLOR, rgb and 0xFFFFFF).apply()
        swatch.background = GradientDrawable().apply {
            setColor(Color.rgb((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF))
            setStroke(dp(1), Color.GRAY)
            cornerRadius = dp(6).toFloat()
        }
    }

    private fun showColor(rgb: Int) {
        updatingHex = true
        hexInput.setText(String.format("#%06X", rgb and 0xFFFFFF))
        updatingHex = false
        saveColor(rgb)
    }

    private fun slider(min: Int, max: Int, value: Int, fmt: (Int) -> String, onChange: (Int) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val label = TextView(this).apply {
            text = fmt(value)
            minWidth = dp(64)
            gravity = Gravity.END
        }
        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = value - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    label.text = fmt(p + min)
                    if (fromUser) onChange(p + min)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        row.addView(bar, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(label)
        return row
    }

    private fun pct(perMille: Int) = String.format("%.1f%%", perMille / 10f)

    private fun title(t: String) = TextView(this).apply {
        text = t
        textSize = 26f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, 0, 0, dp(8))
    }

    private fun header(t: String) = TextView(this).apply {
        text = t
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(24), 0, dp(6))
    }

    private fun body(t: String) = TextView(this).apply {
        text = t
        textSize = 14f
        alpha = 0.8f
        setPadding(0, dp(2), 0, dp(6))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
