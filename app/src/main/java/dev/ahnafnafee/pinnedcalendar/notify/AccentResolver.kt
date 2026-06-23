package dev.ahnafnafee.pinnedcalendar.notify

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.ContextCompat

object AccentResolver {
    fun accentColor(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val res = if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_500
            return ContextCompat.getColor(context, res)
        }
        return 0xFFE07F2C.toInt()
    }
}
