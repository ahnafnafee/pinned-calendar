package com.ahnafnafee.pinnedcalendar.notify

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Intent into the app's system notification settings, where the user can fine-tune the pinned
 * channel — importance, lock-screen visibility, etc. The OS owns those controls; the in-app picker
 * only sets the default level.
 */
object NotificationSettingsIntent {
    fun forApp(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
