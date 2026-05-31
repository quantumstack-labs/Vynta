package com.first_project.chronoai.ui1.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.random.Random

/**
 * Requirement 1 & 3: Architect a High-Fidelity Haptic System for 'Vynta'
 * Semantic Haptic Layer with Dynamic Intensity Control.
 */
class HapticManager(private val context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    enum class VyntaEffect {
        MIC_TRIGGER,      // Double-Tick
        AI_PROCESSING,    // Soft-Pulse
        AI_CRUNCHING,     // Granular "Rain" / "Crunch" effect
        SUCCESS,          // Heartbeat Success
        ERROR,            // Staccato Error
        REFLOW,           // Wave Reflow
        TASK_COMPLETE,    // Confirm-Tick
        CLICK,            // Mechanical Switch
        THROB             // Continuous throb for loading
    }

    /**
     * Requirement 3: DYNAMIC INTENSITY CONTROL
     * Scales amplitude based on provided factor (defaulting to system-aware logic).
     */
    fun play(effect: VyntaEffect, intensityScale: Float = 1.0f) {
        if (!vibrator.hasVibrator()) return

        val scale = intensityScale.coerceIn(0f, 1f)

        when (effect) {
            VyntaEffect.MIC_TRIGGER -> {
                val timings = longArrayOf(0, 40, 60, 40)
                val amplitudes = intArrayOf(0, (255 * scale).toInt(), 0, (255 * scale).toInt())
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
            VyntaEffect.AI_PROCESSING -> {
                vibrator.vibrate(VibrationEffect.createOneShot(30, (100 * scale).toInt()))
            }
            VyntaEffect.AI_CRUNCHING -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val composition = VibrationEffect.startComposition()
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale * 0.3f)
                    vibrator.vibrate(composition.compose())
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, (120 * scale).toInt()))
                }
            }
            VyntaEffect.SUCCESS -> {
                val timings = longArrayOf(0, 30, 80, 40)
                val amplitudes = intArrayOf(0, (150 * scale).toInt(), 0, (255 * scale).toInt())
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
            VyntaEffect.ERROR -> {
                val timings = longArrayOf(0, 50, 40, 50, 40, 50)
                val amplitudes = intArrayOf(0, (255 * scale).toInt(), 0, (255 * scale).toInt(), 0, (255 * scale).toInt())
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
            VyntaEffect.REFLOW -> {
                val timings = longArrayOf(0, 100, 100, 100, 100)
                val amplitudes = intArrayOf(0, (80 * scale).toInt(), (180 * scale).toInt(), (255 * scale).toInt(), (150 * scale).toInt())
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
            VyntaEffect.TASK_COMPLETE -> {
                vibrator.vibrate(VibrationEffect.createOneShot(50, (255 * scale).toInt()))
            }
            VyntaEffect.CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(10, (150 * scale).toInt()))
                }
            }
            VyntaEffect.THROB -> {
                val timings = longArrayOf(0, 100, 50, 100, 50)
                val amplitudes = intArrayOf(0, (60 * scale).toInt(), 0, (60 * scale).toInt(), 0)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0)) // Repeat until stopped
            }
        }
    }

    fun stop() {
        vibrator.cancel()
    }

    fun performClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
