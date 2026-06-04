package com.ricardo.aria

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class ScenePromptDecisionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val promptId = intent.getStringExtra(ScenePromptNotificationManager.EXTRA_PROMPT_ID)
            ?: return
        val decision = intent.getStringExtra(ScenePromptNotificationManager.EXTRA_DECISION)
            ?: return
        val notificationId = intent.getIntExtra(
            ScenePromptNotificationManager.EXTRA_NOTIFICATION_ID,
            ScenePromptNotificationManager.notificationId(promptId)
        )
        val input = Data.Builder()
            .putString(ScenePromptDecisionWorker.KEY_PROMPT_ID, promptId)
            .putString(ScenePromptDecisionWorker.KEY_DECISION, decision)
            .putInt(ScenePromptDecisionWorker.KEY_NOTIFICATION_ID, notificationId)
            .build()

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequest.Builder(ScenePromptDecisionWorker::class.java)
                .setInputData(input)
                .build()
        )
    }
}
