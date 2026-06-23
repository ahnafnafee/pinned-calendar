package dev.ahnafnafee.pinnedcalendar.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.ahnafnafee.pinnedcalendar.MainActivity

/** Pending intent that opens this app's main screen, mirroring a launcher-icon tap. */
object AppLaunch {

    fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, REQUEST_CODE,
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    // All app-launch taps share one intent, so one request code keeps a single PendingIntent record.
    private const val REQUEST_CODE = 100
}
