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
        SUCCESS,          // Success-Pattern
        ERROR,            // Error-Buzz
        TASK_COMPLETE,    // Confirm-Tick
        CLICK             // Mechanical Switch
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
                // Enhanced "Crunching" haptic with robust fallbacks
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val composition = VibrationEffect.startComposition()
                    // Use primitives that are more likely to be supported and noticeable
                    val primitive = if (Random.nextBoolean()) {
                        VibrationEffect.Composition.PRIMITIVE_TICK
                    } else {
                        VibrationEffect.Composition.PRIMITIVE_CLICK
                    }
                    composition.addPrimitive(primitive, scale * Random.nextFloat().coerceIn(0.2f, 0.5f))
                    vibrator.vibrate(composition.compose())
                } else {
                    // Fallback for older devices: very sharp, randomized micro-vibrations
                    val duration = Random.nextLong(5, 12)
                    val amp = Random.nextInt(40, 100)
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, (amp * scale).toInt()))
                }
            }
            VyntaEffect.SUCCESS -> {
                val timings = longArrayOf(0, 50, 50, 100)
                val amplitudes = intArrayOf(0, (200 * scale).toInt(), 0, (255 * scale).toInt())
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
            VyntaEffect.ERROR -> {
                val timings = longArrayOf(0, 80, 100, 80)
                val amplitudes = intArrayOf(0, (255 * scale).toInt(), 0, (255 * scale).toInt())
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
            VyntaEffect.TASK_COMPLETE -> {
                vibrator.vibrate(VibrationEffect.createOneShot(50, (255 * scale).toInt()))
            }
            VyntaEffect.CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10)
                }
            }
        }
    }

    fun performClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
