package com.first_project.chronoai.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.first_project.chronoai.R
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.receiver.NotificationActionReceiver

object LiveNotificationManager {
    private const val CHANNEL_ID = "live_tasks"
    const val NOTIFICATION_ID = 2002

    fun buildNotification(context: Context, task: TaskEntity, progress: Int): Notification {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Live Tasks", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shows live progress for your current task"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Actions
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.first_project.chronoai.ACTION_COMPLETE_TASK"
            putExtra("TASK_ID", task.id)
        }
        val reflowIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.first_project.chronoai.ACTION_REFLOW"
            putExtra("TASK_TITLE", task.title)
        }
        val shieldIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.first_project.chronoai.ACTION_SHIELD"
        }

        val completePI = PendingIntent.getBroadcast(context, task.id, completeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val reflowPI = PendingIntent.getBroadcast(context, task.id + 1, reflowIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val shieldPI = PendingIntent.getBroadcast(context, task.id + 2, shieldIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val pillContent = when (progress) {
            in 0..30 -> "🌱 $progress%"
            in 31..60 -> "⚡ $progress%"
            in 61..85 -> "🎯 $progress%"
            in 86..99 -> "🔥 $progress%"
            else -> "✦ $progress%"
        }
        val trackerEmoji = pillContent.substringBefore(" ")
        val adaptiveTrackerIcon = createEmojiIcon(trackerEmoji)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val nativeBuilder = Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.vynta_sparkle)
                    .setLargeIcon(Icon.createWithResource(context, R.drawable.vynta_sparkle))
                    .setContentTitle(task.title.uppercase())
                    .setContentText(task.schedulingReason ?: "Scheduled for your current flow")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setShowWhen(false)
                    .addAction(Notification.Action.Builder(null, "Reflow", reflowPI).build())
                    .addAction(Notification.Action.Builder(null, "Shield", shieldPI).build())
                    .addAction(Notification.Action.Builder(null, "Complete", completePI).build())

                nativeBuilder.extras.putBoolean("android.requestPromotedOngoing", true)
                nativeBuilder.extras.putCharSequence("android.shortCriticalText", pillContent)
                
                val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
                val progressStyle = progressStyleClass.getDeclaredConstructor().newInstance()
                
                val setProgressMethod = progressStyleClass.getMethod("setProgress", Int::class.javaPrimitiveType)
                setProgressMethod.invoke(progressStyle, progress)

                // 2. Proportional Milestone Segments (Green -> Yellow -> Blue -> Red -> Gold)
                try {
                    val segmentClass = Class.forName("android.app.Notification\$ProgressStyle\$Segment")
                    val segmentConstructor = segmentClass.getConstructor(Int::class.javaPrimitiveType)
                    val setColorMethod = segmentClass.getMethod("setColor", Int::class.javaPrimitiveType)

                    val segments = listOf(
                        segmentConstructor.newInstance(30).apply { setColorMethod.invoke(this, Color.parseColor("#00FF88")) }, // Luminous Green
                        segmentConstructor.newInstance(30).apply { setColorMethod.invoke(this, Color.parseColor("#FFD700")) }, // Neon Gold
                        segmentConstructor.newInstance(25).apply { setColorMethod.invoke(this, Color.parseColor("#00D1FF")) }, // Electric Cyan
                        segmentConstructor.newInstance(14).apply { setColorMethod.invoke(this, Color.parseColor("#FF007A")) }, // Hot Magenta
                        segmentConstructor.newInstance(1).apply { setColorMethod.invoke(this, Color.parseColor("#E0E7FF")) }    // Ultra White
                    )

                    val setProgressSegmentsMethod = progressStyleClass.getMethod("setProgressSegments", List::class.java)
                    setProgressSegmentsMethod.invoke(progressStyle, segments)
                } catch (e: Exception) { Log.w("LiveNotification", "Segments failed") }

                // 3. Set Adaptive Emoji Tracker
                try {
                    val setProgressTrackerIconMethod = progressStyleClass.getMethod("setProgressTrackerIcon", Icon::class.java)
                    if (adaptiveTrackerIcon != null) {
                        setProgressTrackerIconMethod.invoke(progressStyle, adaptiveTrackerIcon)
                    } else {
                        setProgressTrackerIconMethod.invoke(progressStyle, Icon.createWithResource(context, R.drawable.vynta_sparkle))
                    }
                } catch (e: Exception) { Log.w("LiveNotification", "Tracker icon failed") }

                val setStyleMethod = nativeBuilder.javaClass.getMethod("setStyle", Class.forName("android.app.Notification\$Style"))
                setStyleMethod.invoke(nativeBuilder, progressStyle)

                return nativeBuilder.build()
            } catch (e: Exception) {
                Log.e("LiveNotification", "Neural redesign failed", e)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vynta_sparkle)
            .setContentTitle(task.title)
            .setContentText(task.schedulingReason ?: "Scheduled for your current flow")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Reflow", reflowPI)
            .addAction(0, "Shield", shieldPI)
            .addAction(0, "Complete", completePI)

        return builder.build()
    }

    fun showLiveProgressNotification(context: Context, task: TaskEntity, progress: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(context, task, progress))
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        context.stopService(Intent(context, LiveTaskService::class.java))
    }

    private fun createEmojiIcon(emoji: String): Icon? {
        if (Build.VERSION.SDK_INT < 26) return null
        return try {
            val size = 64
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                textSize = 48f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val xPos = canvas.width / 2f
            val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
            canvas.drawText(emoji, xPos, yPos, paint)
            Icon.createWithBitmap(bitmap)
        } catch (e: Exception) {
            null
        }
    }
}
