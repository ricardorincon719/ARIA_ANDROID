package com.ricardo.aria

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.UUID

class ScenePromptDecisionWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val promptId = inputData.getString(KEY_PROMPT_ID) ?: return Result.failure()
        val decision = inputData.getString(KEY_DECISION) ?: return Result.failure()
        val notificationId = inputData.getInt(
            KEY_NOTIFICATION_ID,
            ScenePromptNotificationManager.notificationId(promptId)
        )
        if (decision != "accept" && decision != "cancel") {
            return Result.failure()
        }

        return try {
            val idempotencyKey = "android-${promptId}-${decision}-${UUID.randomUUID()}"
            PearlScenePromptApi.sendDecision(promptId, decision, idempotencyKey)
            ScenePromptNotificationManager.cancel(applicationContext, notificationId)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_PROMPT_ID = "prompt_id"
        const val KEY_DECISION = "decision"
        const val KEY_NOTIFICATION_ID = "notification_id"
    }
}
