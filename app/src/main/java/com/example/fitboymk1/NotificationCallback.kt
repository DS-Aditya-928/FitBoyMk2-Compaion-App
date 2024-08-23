package com.example.fitboymk1

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

var bd: BluetoothDevice? = null
val mListener = KeyChanged()
//var btGatt: BluetoothGatt? = null

class NotificationListener : NotificationListenerService()
{
    companion object
    {
        private var ctx: NotificationListener? = null

        fun mainContext(): NotificationListener?
        {
            return (ctx)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i("UB", "Unbound")
        return super.onUnbind(intent)
    }

    override fun onListenerDisconnected()
    {
        Log.i("Listener disconnected", "LD")
        super.onListenerDisconnected()
    }

    @SuppressLint("MissingPermission")
    override fun onListenerConnected() {
        ctx = this

        val chan = NotificationChannel(
            "channelId",
            "channelName", NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.Blue.toArgb()
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)

        tDN = Notification.Builder(this, "channelId")
            .setContentTitle("title")
            .setContentText("subject")
            .build()

        startForeground(1, tDN)
        Log.i("Task status", "Foreground registered")

        mediaManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaManager?.removeOnMediaKeyEventSessionChangedListener (mListener)
        mediaManager?.addOnMediaKeyEventSessionChangedListener(this.mainExecutor, mListener)
        aM = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mReceiver, filter);

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        val btAdapter = btManager.adapter

        bd = btAdapter.getRemoteDevice("EC:62:60:32:C6:4E") as BluetoothDevice

        //Log.i("C", "Calling connectGatt")
        btGatt = bd?.connectGatt(applicationContext, true, BTCallBack()) as BluetoothGatt
        Log.i("Dev", "AUTOCONNECT SET")

        super.onListenerConnected()
    }

    @SuppressLint("MissingPermission")
    override fun onNotificationPosted(sbn: StatusBarNotification)
    {
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            //Ignore the notification
            return
        }
        val mNotification = sbn.notification
        val extras = mNotification.extras
        //bundle2string(extras)?.let { Log.i("NL", it) }

        val packageManager = applicationContext.packageManager
        val appName = packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(
                sbn.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        ) as String

        val nTitle = extras.getString("android.title")
        val nSubText = extras.getString("android.subText")

        var sendMsg = "<0>$appName<1>$nTitle<2>$nSubText<3>"

        if(extras.containsKey("android.messages")) {
            sendMsg += "T<4>\n"
            val pArray: Array<Parcelable> =
                extras.getParcelableArray("android.messages") as Array<Parcelable>
            val messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(pArray)

            for (messageI in messages) {
                sendMsg += messageI.senderPerson?.name.toString() + ":" + messageI.text.toString() + "\n"
            }
        }

        else
        {
            sendMsg += "D<4>"

            val bigText = extras.getString("android.bigText").toString()
            val text = extras.getString("android.text").toString()
            var FT = ""

            if(bigText != null)
            {
                FT = bigText
            }

            else if(text != null)
            {
                FT = text
            }

            if(FT.length > 128)
            {
                FT = FT.subSequence(0, 125).toString()
                FT += "..."
            }

            sendMsg += FT
        }

        val nId = sbn.key

        sendMsg.filter { it.code <= 127 }
        sendMsg += "<5>$nId"

        while(!notBufClean)
        {
            Thread.sleep(1)
        }

        Log.i("SEND MSG", sendMsg)

        if(connected and (notBufC != null))
        {
            notBufClean = false
            notDelBufClean = false
            btGatt?.writeCharacteristic(notBufC!!, sendMsg.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onNotificationRemoved(sbn: StatusBarNotification?)
    {
        Log.i("SEND MSG DEL", sbn?.key!!)
        if(connected and (notDelBufC != null) and (sbn.key != null))
        {
            while(!notDelBufClean)
            {
                Thread.sleep(1)
            }
            notDelBufClean = false
            btGatt?.writeCharacteristic(notDelBufC!!, sbn.key!!.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
        super.onNotificationRemoved(sbn)
    }
}