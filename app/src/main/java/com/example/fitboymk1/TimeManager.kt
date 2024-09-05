package com.example.fitboymk1

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.example.btmodule.BTInstance
import com.example.btmodule.DSCallback
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

val TIME_UUID: UUID = UUID.fromString("93c37a10-1f37-11ee-be56-0242ac120002")

class TimeGCallback : DSCallback()
{
    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, newState: Int)
    {
        if ((gatt != null) and (timeBTInstance.characteristic != null))
        {
            var unixTime = (Calendar.getInstance().timeInMillis/1000)
            val tz = TimeZone.getDefault() as TimeZone
            unixTime += (tz.getOffset(Calendar.getInstance().timeInMillis)/1000)

            val utString = unixTime.toString()
            timeBTInstance.writeCharacteristic(utString.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            //timeBTInstance.characteristic?.let { gatt?.writeCharacteristic(it,  utString.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) }
            Log.i("Time", "Set $utString " + unixTime + " " + tz.getOffset(unixTime)/1000)
        }
    }
}

val timeBTInstance : BTInstance = BTInstance(TIME_UUID, TimeGCallback())