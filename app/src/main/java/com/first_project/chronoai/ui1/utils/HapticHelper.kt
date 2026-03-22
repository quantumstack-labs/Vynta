package com.first_project.chronoai.ui1.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class VyntaHapticEngine(context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Physics-based magnetic snap for scrolling/paging
     */
    fun magneticSnap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f, 20)
                .compose()
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /**
     * Mechanical switch feel for buttons (Press)
     */
    fun mechanicalPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                .compose()
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 200))
        }
    }

    /**
     * Mechanical switch feel for buttons (Release)
     */
    fun mechanicalRelease() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f)
                .compose()
            vibrator.vibrate(effect)
        }
    }

    /**
     * Organic heartbeat pulse (lub-dub) for background intelligence
     */
    fun intelligenceHeartbeat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.4f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.7f, 150)
                .compose()
            vibrator.vibrate(effect)
        } else {
            val timings = longArrayOf(0, 40, 150, 60)
            val amplitudes = intArrayOf(0, 30, 0, 50)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(timings, -1)
            }
        }
    }

    /**
     * Rewarding sparkle sequence for success
     */
    fun successSparkle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.6f, 50)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 0.8f, 100)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 50)
                .compose()
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(longArrayOf(0, 20, 50, 20, 50, 40), -1)
        }
    }
}

@Composable
fun rememberVyntaHaptic(): VyntaHapticEngine {
    val context = LocalContext.current
    return remember(context) { VyntaHapticEngine(context) }
}

@Composable
fun rememberHapticFeedback(): () -> Unit {
    val haptic = rememberVyntaHaptic()
    return remember(haptic) { { haptic.mechanicalPress() } }
}
