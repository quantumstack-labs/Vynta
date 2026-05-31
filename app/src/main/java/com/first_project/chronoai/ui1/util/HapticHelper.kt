package com.first_project.chronoai.ui1.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticHelper {
    var hapticsEnabled: Boolean = true

    fun playClick(view: View) {
        if (!hapticsEnabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun playEffect(context: Context, type: String) {
        if (!hapticsEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        when (type) {
            "START_RECORDING" -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
            "STOP_RECORDING" -> vibrator.vibrate(VibrationEffect.createOneShot(60, 200))
            "AI_START" -> vibrator.vibrate(VibrationEffect.createOneShot(30, 100))
            "SUCCESS" -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 100), -1))
            "ERROR" -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 100, 80), -1))
        }
    }
}
