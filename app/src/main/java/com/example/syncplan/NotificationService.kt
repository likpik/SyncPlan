package com.example.syncplan.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.syncplan.R
import com.example.syncplan.MainActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class NotificationService(private val context: Context) {
    companion object {
        private const val CHANNEL_ID_EVENTS = "events_channel"
        private const val CHANNEL_ID_MESSAGES = "messages_channel"
        private const val CHANNEL_ID_REMINDERS = "reminders_channel"
        private const val EVENT_REMINDER_WORK = "event_reminder_work"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val eventsChannel = NotificationChannel(
                CHANNEL_ID_EVENTS,
                "Wydarzenia",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Powiadomienia o wydarzeniach"
                enableLights(true)
                lightColor = Color.BLUE
            }

            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Wiadomości",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o nowych wiadomościach"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Przypomnienia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Przypomnienia o nadchodzących wydarzeniach"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(eventsChannel, messagesChannel, remindersChannel)
            )
        }
    }

    fun scheduleEventReminder(
        eventId: String,
        title: String,
        message: String,
        time: LocalDateTime
    ) {
        val now = LocalDateTime.now()
        if (time.isBefore(now)) {
            return
        }

        val delay = Duration.between(now, time)
        val data = workDataOf(
            "event_id" to eventId,
            "title" to title,
            "message" to message
        )

        val reminderWork = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag(eventId)
            .addTag(EVENT_REMINDER_WORK)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reminder_$eventId",
                ExistingWorkPolicy.REPLACE,
                reminderWork
            )
    }

    fun cancelEventReminders(eventId: String) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(eventId)
    }

    fun sendNotification(
        userId: String,
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_EVENTS,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Używamy systemowej ikony
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
        }
    }

    fun sendEventReminderNotification(
        eventId: String,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("event_id", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(eventId.hashCode(), notification)
        }
    }

    fun sendChatMessageNotification(
        chatId: String,
        senderId: String,
        senderName: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chat_id", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(senderName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(chatId.hashCode(), notification)
        }
    }
}

class EventReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val eventId = inputData.getString("event_id") ?: return Result.failure()
        val title = inputData.getString("title") ?: return Result.failure()
        val message = inputData.getString("message") ?: return Result.failure()

        val notificationService = NotificationService(applicationContext)
        notificationService.sendEventReminderNotification(eventId, title, message)

        return Result.success()
    }
}
