package com.rommy.youtorrent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TorrentService : Service() {
    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        TorrentEngine.get(this).start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_MAGNET -> intent.getStringExtra(EXTRA_MAGNET)?.let(TorrentEngine.get(this)::addMagnet)
            ACTION_TOGGLE -> intent.getStringExtra(EXTRA_HASH)?.let(TorrentEngine.get(this)::toggle)
            ACTION_REMOVE -> intent.getStringExtra(EXTRA_HASH)?.let(TorrentEngine.get(this)::remove)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Torrent downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("YouTorrent")
            .setContentText("Torrent engine is running")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    companion object {
        const val ACTION_ADD_MAGNET = "com.rommy.youtorrent.ADD_MAGNET"
        const val ACTION_TOGGLE = "com.rommy.youtorrent.TOGGLE"
        const val ACTION_REMOVE = "com.rommy.youtorrent.REMOVE"
        const val EXTRA_MAGNET = "magnet"
        const val EXTRA_HASH = "hash"
        private const val CHANNEL_ID = "torrent_engine"
        private const val NOTIFICATION_ID = 1001
    }
}
