package com.leminect.stranzee.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.leminect.stranzee.R
import com.leminect.stranzee.view.MainActivity
import java.util.*

data class Notification(
    val title: String,
    val message: String,
    val senderId: String,
    val receiverId: String,
    val type: String,
) {
    companion object {
        const val ADMIN_CHANNEL_ID: String = "admin_channel"
    }

    fun show(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random().nextInt(3000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupChannels(notificationManager)
        }

        val intentActivity: Intent = Intent(context, MainActivity::class.java)
        if(type == "chat") {
            intentActivity.putExtra("notificationDestination", "chat")
            intentActivity.putExtra("senderId", senderId)
        }
        intentActivity.action = Intent.ACTION_MAIN
        intentActivity.addCategory(Intent.CATEGORY_LAUNCHER)
        intentActivity.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK


        val contentIntent = PendingIntent.getActivity(context,
            (Math.random() * 100).toInt(), intentActivity, 0)

        val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            //.setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(notificationSoundUri)
            .setContentIntent(contentIntent)

        //Set notification color to match your app color template
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.color = ContextCompat.getColor(context, R.color.main_blue)
        }

        notificationManager.notify(notificationID, notificationBuilder.build())
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupChannels(notificationManager: NotificationManager?) {
        val adminChannelName: CharSequence = "New notification"
        val adminChannelDescription = "Device to device notification"
        val adminChannel = NotificationChannel(ADMIN_CHANNEL_ID,
            adminChannelName,
            NotificationManager.IMPORTANCE_HIGH)
        adminChannel.description = adminChannelDescription
        adminChannel.enableLights(true)
        adminChannel.lightColor = Color.RED
        adminChannel.enableVibration(true)
        notificationManager?.createNotificationChannel(adminChannel)
    }
}