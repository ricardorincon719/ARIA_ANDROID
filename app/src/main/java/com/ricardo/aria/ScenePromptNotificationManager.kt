package com.ricardo.aria

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object ScenePromptNotificationManager {
    const val CHANNEL_ID = "pearl_scene_prompts"
    const val EXTRA_PROMPT_ID = "prompt_id"
    const val EXTRA_DECISION = "decision"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Escenas PEARL",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Propuestas de escenas aprendidas que requieren consentimiento."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showPrompt(context: Context, prompt: PearlScenePrompt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationId = notificationId(prompt.id)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(prompt.title)
            .setContentText(prompt.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(prompt.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Aceptar",
                decisionIntent(context, prompt.id, "accept", notificationId)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancelar",
                decisionIntent(context, prompt.id, "cancel", notificationId)
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun notificationId(promptId: String): Int = promptId.hashCode() and Int.MAX_VALUE

    private fun decisionIntent(
        context: Context,
        promptId: String,
        decision: String,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, ScenePromptDecisionReceiver::class.java).apply {
            putExtra(EXTRA_PROMPT_ID, promptId)
            putExtra(EXTRA_DECISION, decision)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val requestCode = "${promptId}_$decision".hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
