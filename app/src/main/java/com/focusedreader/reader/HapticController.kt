package com.focusedreader.reader

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.focusedreader.data.HapticMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticController @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val punctuation = setOf('.', '!', '?', ',', ';', ':')

    fun tick(word: String, mode: HapticMode, intensityPct: Int) {
        if (mode == HapticMode.OFF || intensityPct <= 0) return
        if (mode == HapticMode.PER_PUNCTUATION && word.lastOrNull() !in punctuation) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val amplitude = (intensityPct / 100.0 * 255).toInt().coerceIn(1, 255)
        v.vibrate(VibrationEffect.createOneShot(15L, amplitude))
    }
}
