package com.example.fitboymk1

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

var notBufC:BluetoothGattCharacteristic? = null
var notDelBufC:BluetoothGattCharacteristic? = null
var fbDel:BluetoothGattCharacteristic? = null
var deetsCharacteristic:BluetoothGattCharacteristic? = null
var mcCharacteristic: BluetoothGattCharacteristic? = null
var timeC: BluetoothGattCharacteristic? = null

var notBufClean = true
var notDelBufClean = true

class BTCallBack : BluetoothGattCallback()
{
    private val x = Random.nextInt()

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        if(characteristic == notBufC)
        {
            notBufClean = true
            notDelBufClean = true
            Log.d("del", "nB")
        }

        else if(characteristic == notDelBufC)
        {
            notDelBufClean = true
            Log.d("del", "nD")
        }

        else if(characteristic == timeC)
        {
            sendDetes(lastController)
        }

        super.onCharacteristicWrite(gatt, characteristic, status)
    }

    @SuppressLint("MissingPermission")
    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        //Log.i("Media info", mediaManager.ge)

        val gattService = gatt?.getService(SERVICE_UUID)
        timeC = gattService?.getCharacteristic(TIME_UUID)

        if (timeC != null)
        {
            var unixTime = (Calendar.getInstance().timeInMillis/1000)
            val tz = TimeZone.getDefault() as TimeZone
            unixTime += (tz.getOffset(Calendar.getInstance().timeInMillis)/1000)

            val utString = unixTime.toString()
            gatt?.writeCharacteristic(timeC!!,  utString.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            //Log.i("Time", "Set $utString " + unixTime + " " + tz.getOffset(unixTime)/1000)
        }

        //populate global var
        notBufC = gattService?.getCharacteristic(NOTBUF_UUID)
        notDelBufC = gattService?.getCharacteristic(NOTDELBUF_UUID)
        fbDel = gattService?.getCharacteristic(FBDEL_UUID)
        deetsCharacteristic = gattService?.getCharacteristic(MUSICDEETS_UUID)
        mcCharacteristic = gattService?.getCharacteristic(MUSICCONTROL_UUID)

        Thread.sleep(100)

        super.onDescriptorWrite(gatt, descriptor, status)
    }

    @SuppressLint("MissingPermission")
    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        Log.d("MTU", mtu.toString())
        gatt?.discoverServices()
        super.onMtuChanged(gatt, mtu, status)
    }

    @SuppressLint("MissingPermission")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        Log.i("C", "CHANGE")

        if(characteristic == fbDel)
        {
            val v = value.decodeToString()
            NotificationListener.mainContext()?.cancelNotification(v)
            Log.i("C Change", v)
            //usually only called when we r notified of a notification being deleted.
        }

        else if(characteristic == mcCharacteristic)
        {
            Log.i("j", "P")
            val eventtime = SystemClock.uptimeMillis()

            val downEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
            aM?.dispatchMediaKeyEvent(downEvent)

            val upEvent =
                KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
            aM?.dispatchMediaKeyEvent(upEvent)
        }

        super.onCharacteristicChanged(gatt, characteristic, value)
    }

    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        Log.i("Services", "Discovered!")
        val gattService = gatt?.getService(SERVICE_UUID)

        val fbdC = gattService?.getCharacteristic(FBDEL_UUID)
        val mcC = gattService?.getCharacteristic(MUSICCONTROL_UUID)

        if(fbdC != null)
        {
            gatt.setCharacteristicNotification(fbdC, true)
            val descriptor: BluetoothGattDescriptor = fbdC.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)

            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        }

        if(mcC != null)
        {
            Log.i("MCC", "SET")
            gatt.setCharacteristicNotification(mcC, true)
            val descriptor: BluetoothGattDescriptor = mcC.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)

            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        }

        super.onServicesDiscovered(gatt, status)
    }
}


val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR
            )

            when (state) {
                BluetoothAdapter.STATE_TURNING_OFF->
                {
                    Log.i("BT STATUS", "OFF")
                    //connected = false
                }
            }
        }
    }
}