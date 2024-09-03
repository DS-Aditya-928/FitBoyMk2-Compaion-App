package com.example.fitboymk1

import android.annotation.SuppressLint
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fitboymk1.ui.theme.FitBoyMk1Theme
import kotlinx.coroutines.delay
import java.util.UUID

val SERVICE_UUID: UUID = UUID.fromString("1f55d926-12bb-11ee-be56-0242ac120002")
val NOTBUF_UUID: UUID = UUID.fromString("05590c96-12bb-11ee-be56-0242ac120002")
val NOTDELBUF_UUID: UUID = UUID.fromString("19e04166-12bb-11ee-be56-0242ac120002")
val TIME_UUID: UUID = UUID.fromString("93c37a10-1f37-11ee-be56-0242ac120002")
val FBDEL_UUID: UUID = UUID.fromString("c533a7ba-272e-11ee-be56-0242ac120002")

val CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

var tDN : Notification? = null
//var nLS: NotificationListenerService? = null
var mediaManager: MediaSessionManager? = null
var aM: AudioManager? = null
val mainContext = null

var cInit = false

class MainActivity : ComponentActivity() {
    override fun onDestroy() {
        Log.i("MAIN ACTIVITY", "DEAD");
        super.onDestroy()
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NotificationManagerCompat.getEnabledListenerPackages(this)
                .contains(packageName)
        ) {        //ask for permission
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf("android.permission.ACCESS_COARSE_LOCATION", "android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"),
            2
        )

        setContent {
            FitBoyMk1Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                }
            }
        }
    }
}

