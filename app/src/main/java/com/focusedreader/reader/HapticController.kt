package com.focusedreader.reader

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.focusedreader.data.HapticMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticController @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    companion object { private const val TAG = "HapticController" }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val audioAttrs by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private val vibrationAttrs by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
        } else null
    }

    private val punctuation = setOf('.', '!', '?', ',', ';', ':')

    fun tick(word: String, mode: HapticMode, intensityPct: Int) {
        if (mode == HapticMode.OFF || intensityPct <= 0) {
            Log.d(TAG, "tick skipped mode=$mode intensity=$intensityPct")
            return
        }
        if (mode == HapticMode.PER_PUNCTUATION && word.lastOrNull() !in punctuation) return

        val v = vibrator
        if (v == null || !v.hasVibrator()) {
            Log.w(TAG, "vibrator unavailable: v=$v hasVibrator=${v?.hasVibrator()}")
            return
        }

        val effect = pickEffect(intensityPct)
        Log.d(TAG, "tick word=$word mode=$mode intensity=$intensityPct effect=$effect")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && vibrationAttrs != null) {
            v.vibrate(effect, vibrationAttrs!!)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, audioAttrs)
        }
    }

    private fun pickEffect(intensityPct: Int): VibrationEffect {
        return when {
            intensityPct <= 11 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            intensityPct <= 22 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
    }
}
