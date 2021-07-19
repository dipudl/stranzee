package com.leminect.stranzee.network

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.leminect.stranzee.R
import com.leminect.stranzee.model.Notification
import com.leminect.stranzee.utility.getFromSharedPreferences
import com.leminect.stranzee.utility.getUidFromSharedPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessagingService"

    override fun onNewToken(token: String) {
        val userDetail = getFromSharedPreferences(applicationContext)
        if (userDetail.second.userId.isNotEmpty() && userDetail.first.isNotEmpty()) {
            GlobalScope.launch {
                try {
                    val result = StrangeeApi.retrofitService.refreshFcmToken(
                        "Bearer ".plus(userDetail.first),
                        userDetail.second.userId,
                        token
                    )
                    Log.i(TAG, result.toString())
                } catch (t: Throwable) {
                    Log.i(TAG, "FCM_Refresh_Token_Error :: $t")
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        try {
            val prefs =
                applicationContext.getSharedPreferences(getString(R.string.shared_prefs_name),
                    MODE_PRIVATE)

            // Check if message contains a data payload.
            if (remoteMessage.data.isNotEmpty()) {
                Log.i(TAG, remoteMessage.notification.toString())
                val notificationType = remoteMessage.data["notificationType"]!!

                if(notificationType == "push") {
                    remoteMessage.notification?.let {
                        val notification = Notification(
                            it.title?:"",
                            it.body?:"",
                            "admin",
                            "user",
                            notificationType
                        )
                        notification.show(applicationContext)
                    }
                } else {
                    val senderId = remoteMessage.data["senderId"]!!
                    val receiverId = remoteMessage.data["receiverId"]!!
                    val userId = prefs.getString(getString(R.string.prefs_user_id), "")!!

                    if ((userId.isNotEmpty() && receiverId == userId) && (notificationType != "chat" || prefs?.getString(
                            getString(R.string.prefs_current_chat_user_id),
                            null) != senderId)
                    ) {
                        val notification = Notification(
                            remoteMessage.data["title"]!!,
                            remoteMessage.data["body"]!!,
                            senderId,
                            receiverId,
                            notificationType
                        )
                        notification.show(applicationContext)
                    }
                }
            }

            // Check if message contains a notification payload.
            /*remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            }*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}