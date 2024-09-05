package com.example.fitboymk1

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.example.btmodule.BTInstance
import com.example.btmodule.DSCallback
import java.util.UUID
import kotlin.random.Random

var mediaAppName = ""

var ccB = DeetsCallback()
var deetsClean = true
var sentDeets = false

val MUSICDEETS_UUID: UUID = UUID.fromString("5df4d2b0-a927-11ee-a506-0242ac120002")
val MUSICCONTROL_UUID : UUID = UUID.fromString("6ddb28be-a927-11ee-a506-0242ac120002")

@SuppressLint("MissingPermission")
fun sendDetes(mc: MediaController?)
{
    var album = ""
    var trackName = ""
    var artist = ""
    var trackLength = 0L
    var play = 0
    var cPos = 0L

    if(mc != null)
    {
        var metadata = mc.metadata

        if(metadata != null)
        {
            album = if(metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) == null) " " else metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            artist = if(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) == null) " " else metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            trackName = if(metadata.getString(MediaMetadata.METADATA_KEY_TITLE) == null) " " else metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            trackLength = if(metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) == null) 0 else metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        }

        var pbS = mc.playbackState

        if(pbS != null)
        {
            cPos = pbS.position
            play = -1 * (pbS.state == PlaybackState.STATE_PAUSED).compareTo(true)
            if(cPos < 0)
            {
                cPos = 0
                play = 0
            }
        }

        //trackN = if(metadata.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER) == null) " " else (metadata.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER).toString())
        //trackN = if(metadata.containsKey("com.google.android.music.mediasession.METADATA_KEY_QUEUE_POSITION")) metadata.getLong("com.google.android.music.mediasession.METADATA_KEY_QUEUE_POSITION").toString() else "0aa"
        //totalT = if(metadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS) == null) " " else metadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS).toString()
    }

    val toSend = "<AD>$trackName<1>$artist<2>$album<3>$trackLength<4>$cPos<5>$play"

    Log.i("TS", toSend)
    sentDeets = true

    //Log.i("<KEYS", metadata?.keySet().toString())

    musicdeetsBTInstance.writeCharacteristic(toSend.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)

    /*
    if((deetsCharacteristic != null) and connected)
    {
        deetsClean = false
        //btGatt?.writeCharacteristic(deetsCharacteristic!!, toSend.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    }


     */
    deetsClean = true
}


var lastController: MediaController? = null
public class KeyChanged: MediaSessionManager.OnMediaKeyEventSessionChangedListener
{
    @SuppressLint("MissingPermission")
    override fun onMediaKeyEventSessionChanged(p0: String, p1: MediaSession.Token?)
    {
        //also wake phone up.
        val packageManager = NotificationListener.mainContext()?.packageManager
        mediaAppName = packageManager?.getApplicationLabel(
            packageManager.getApplicationInfo(
                p0,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        ) as String

        Log.i("KC", "$mediaAppName " + p1.toString() + " " + Random.nextInt().toString())

        Thread.sleep(500)

        val mcList = mediaManager?.getActiveSessions(ComponentName("com.example.fitboymk1", ".NotificationListener"));

        if(p1 == null)
        {
            lastController?.unregisterCallback(ccB)
            //controller deregged. send kill cmd

            musicdeetsBTInstance.writeCharacteristic("KILL".toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)

            /*
            if((deetsCharacteristic != null) and connected)
            {
                btGatt?.writeCharacteristic(deetsCharacteristic!!, "KILL".toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            }
            */

            return
        }

        if(mcList != null)
        {
            Log.i("S", "Searching for sessions")
            for(i in mcList)
            {
                Log.i("S", i.packageName)
                if(i.sessionToken == p1)
                {
                    Log.i("KC", "Found media controller")
                    if(lastController != null)
                    {
                        lastController?.unregisterCallback(ccB)
                    }
                    sentDeets = false
                    i.unregisterCallback(ccB)
                    i.registerCallback(ccB)
                    lastController = i

                    Thread.sleep(100)
                    if(!sentDeets)
                    {
                        val mD = i.metadata
                        sendDetes(i)
                    }
                }
            }
        }
    }
}

public class DeetsCallback: MediaController.Callback()
{
    val x = Random.nextInt()
    override fun onPlaybackStateChanged(state: PlaybackState?)
    {
        sendDetes(lastController)
        //Log.i("PS", state.toString())
        super.onPlaybackStateChanged(state)
    }

    @SuppressLint("MissingPermission", "WrongConstant")
    override fun onMetadataChanged(metadata: MediaMetadata?)
    {
        sendDetes(lastController)

        super.onMetadataChanged(metadata)
    }

    @SuppressLint("MissingPermission")
    override fun onSessionDestroyed() {
        Log.i("Sesh", "sesh destryed");

        musicdeetsBTInstance.writeCharacteristic("KILL".toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        //send message to watch to disable media control.
        /*
        if((deetsCharacteristic != null) and connected)
        {
            btGatt?.writeCharacteristic(deetsCharacteristic!!, "KILL".toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }

         */
        super.onSessionDestroyed()
    }
}

class mdSCB: DSCallback()
{
    override fun onConnectionStateChange(gatt: BluetoothGatt?, newState: Int) {
        if(gatt != null)
        {
            sendDetes(lastController)
        }
    }
}

class musicControlCB : DSCallback()
{
    override fun onCharacteristicChange(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val v = value.decodeToString()
        if(v.compareTo("1") == 0)
        {
            val eventtime = SystemClock.uptimeMillis()

            val downEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
            aM?.dispatchMediaKeyEvent(downEvent)

            val upEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
            aM?.dispatchMediaKeyEvent(upEvent)
        }

        else if(v.compareTo("2") == 0)
        {
            val eventtime = SystemClock.uptimeMillis()

            val downEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0)
            aM?.dispatchMediaKeyEvent(downEvent)

            val upEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0)
            aM?.dispatchMediaKeyEvent(upEvent)
        }

        else if(v.compareTo("3") == 0)
        {
            val eventtime = SystemClock.uptimeMillis()

            val downEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0)
            aM?.dispatchMediaKeyEvent(downEvent)

            val upEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0)
            aM?.dispatchMediaKeyEvent(upEvent)
        }


    }
}

val musicdeetsBTInstance : BTInstance = BTInstance(MUSICDEETS_UUID, mdSCB())
val musicControlBTInstance : BTInstance = BTInstance(MUSICCONTROL_UUID, musicControlCB(), true)