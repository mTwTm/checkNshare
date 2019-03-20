package org.mozilla.check.n.share.entry

import android.os.IBinder
import android.util.Log
import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.Service
import android.content.*
import androidx.core.app.NotificationCompat
import org.mozilla.check.n.share.activity.ShareProxyActivity

class ClipService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") // We never bind
    }

    private val channelname = "relocation_service"
    private val id = 987

    // Configure the notification channel if needed
    private fun configForegroundChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // NotificationChannel API is only available for Android O and above, so we need to add the check here so IDE won't complain
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "CNS"
            val notificationChannel = NotificationChannel(channelname, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun startForeground() {
        val notificationChannelId: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            configForegroundChannel(this)
            notificationChannelId = channelname
        } else {
            notificationChannelId = "not_used_notification_id"
        }
        val builder = NotificationCompat.Builder(applicationContext, notificationChannelId)

        val notification = builder
            .build()
        startForeground(id, notification)
    }

    var last: ClipData? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        val mCM = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val function = fun() {
            val thisClip = mCM.primaryClip
            // For some unknown reason call back nearly always fire twice, dedup.
            if (thisClip?.toString() == last?.toString()) {
                Log.e("Clipboard", "dup!")
                return
            }
            last = thisClip
            val activityIntent = Intent(Intent.ACTION_SEND)
            activityIntent.putExtra(Intent.EXTRA_TEXT, thisClip.getItemAt(0).text)
            activityIntent.type = "text/plain"
            activityIntent.component = ComponentName(this@ClipService, ShareProxyActivity::class.java)
            activityIntent.flags += Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(activityIntent)
        }
        mCM.addPrimaryClipChangedListener(function)
        return START_STICKY
    }
}