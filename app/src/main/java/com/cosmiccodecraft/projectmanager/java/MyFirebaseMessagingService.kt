package com.cosmiccodecraft.projectmanager.java

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cosmiccodecraft.projectmanager.R
import com.cosmiccodecraft.projectmanager.activities.MainActivity
import com.cosmiccodecraft.projectmanager.activities.SignInActivity
import com.cosmiccodecraft.projectmanager.firebase.FireStore
import com.cosmiccodecraft.projectmanager.utils.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    // two types of messsages data and notification
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "From: ${message.from}")
        println("********** MyFirebaseMessaging onMessageReceived ${message.data}")

        message.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${message.data}")

            val title = message.data[Constants.FCM_KEY_TITLE]!!
            val messageBody = message.data[Constants.FCM_KEY_MESSAGE]!!

            sendNotification(title, messageBody)
        }

        message.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }


    }

    override fun onNewToken(token: String) {
        // called if the instance ID token is updated this can occur if the security of the previous token had been compromised
        // or if the token is expired also invoked if a initial token is generated
        super.onNewToken(token)

        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // send the token to the server
        val sharedPreferences =
            this.getSharedPreferences(Constants.PROJECTMANAGER_PREFS, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(Constants.FCM_TOKEN, token)
        editor.apply()
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = if (FireStore().getCurrentUserId().isNotEmpty()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, SignInActivity::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = this.resources.getString(R.string.default_notification_channel_id)
        val defaultRingTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat
            .Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultRingTone)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel Project Manager",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}