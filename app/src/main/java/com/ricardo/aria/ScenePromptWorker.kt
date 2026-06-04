package com.ricardo.aria

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ScenePromptWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            val prompts = PearlScenePromptApi.fetchPendingPrompts()
            prompts.take(MAX_NOTIFICATIONS).forEach { prompt ->
                ScenePromptNotificationManager.showPrompt(applicationContext, prompt)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private companion object {
        const val MAX_NOTIFICATIONS = 5
    }
}
