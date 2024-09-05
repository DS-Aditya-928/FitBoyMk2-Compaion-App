package com.example.btmodule

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

open class DSCallback
{
    @Override
    open fun onConnectionStateChange(gatt: BluetoothGatt?, newState : Int)
    {

    }

    @Override
    open fun onCharacteristicChange(characteristic: BluetoothGattCharacteristic, value: ByteArray)
    {

    }
}

class BTInstance(uuid: UUID, bgCallback: DSCallback?, enableNotification: Boolean = false)
{
    internal var uuid : UUID = UUID.randomUUID()
    internal var enableNotification : Boolean = false
    internal var bgCallback : DSCallback? = null
    var characteristic : BluetoothGattCharacteristic? = null

    internal var writeClean : Boolean = true

    init
    {
        this.uuid = uuid
        this.enableNotification = enableNotification
        this.bgCallback = bgCallback
        BTManager.btInstanceArray += this
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    fun writeCharacteristic(v : ByteArray, writeType : Int)
    {
        while(!BTManager.writeClean or !this.writeClean)
        {
            Thread.sleep(1)
        }

        if((characteristic != null) and BTManager.connected)
        {
            BTManager.writeClean = false
            this.writeClean = false
            BTManager.btGatt?.writeCharacteristic(characteristic!!, v, writeType)
        }
    }
}