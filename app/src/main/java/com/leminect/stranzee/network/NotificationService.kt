package com.leminect.stranzee.network

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.leminect.stranzee.R
import com.leminect.stranzee.model.Notification
import io.socket.emitter.Emitter


class NotificationService : Service() {
    private var prefs: SharedPreferences? = null
    private var userId: String? = null
    private var token: String? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(getString(R.string.shared_prefs_name), MODE_PRIVATE)

        userId = prefs?.getString(getString(R.string.prefs_user_id), null)
        token = prefs?.getString(getString(R.string.prefs_token), null)
        if (userId != null && token != null) {
            // prefs?.edit()?.putBoolean(getString(R.string.prefs_notification_service), true)?.apply()
            SocketManager.getSocket()?.emit("subscribe", Gson().toJson(
                RoomData(userId!!, "", "notification", token!!)
            ))
            SocketManager.getSocket()?.on("notification", onNewNotification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val newUserId = intent?.extras?.getString("userId")
        val newToken = intent?.extras?.getString("token")

        if (userId == null || token == null) {
            if (newUserId != null) {
                userId = newUserId
            }
            if (newToken != null) {
                token = newToken
            }

            if (userId != null && token != null) {
                // prefs?.edit()?.putBoolean(getString(R.string.prefs_notification_service), true)?.apply()
                SocketManager.getSocket()?.emit("subscribe", Gson().toJson(
                    RoomData(userId!!, "", "notification", token!!)
                ))
                SocketManager.getSocket()?.on("notification", onNewNotification)
            }
        }

        return START_STICKY
    }

    val onNewNotification = Emitter.Listener {
        val notification: Notification =
            SocketManager.gson.fromJson(it[0].toString(), Notification::class.java)

        if (notification.receiverId == userId) {
            if (notification.type != "chat" || prefs?.getString(getString(R.string.prefs_current_chat_user_id),
                    null) != notification.senderId
            ) {
                notification.show(applicationContext)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val serviceIntent = Intent(applicationContext, NotificationService::class.java)
        serviceIntent.putExtra("userId", prefs?.getString(getString(R.string.prefs_user_id), null))
        serviceIntent.putExtra("token", prefs?.getString(getString(R.string.prefs_token), null))
        startService(serviceIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        SocketManager.getSocket()?.off("notification")
        if (userId != null && token != null) {
            SocketManager.getSocket()?.emit("unsubscribe", Gson().toJson(
                RoomData(userId!!, "", "notification", token!!)
            ))
        }
        // prefs?.edit()?.putBoolean(getString(R.string.prefs_notification_service), false)?.apply()
    }
}