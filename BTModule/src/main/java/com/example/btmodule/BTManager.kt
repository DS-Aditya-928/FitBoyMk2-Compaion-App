package com.example.btmodule

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import java.util.UUID

class BTWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams)
{
    companion object
    {
        var taskDone: Boolean = true
    }
    private fun waitTD()
    {
        Thread.sleep(100)
        while(!taskDone){Thread.sleep(1)}
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    override fun doWork(): Result
    {
        Log.i("BTWorker", "Start")

        waitTD()
        taskDone = false
        BTManager.btGatt?.requestMtu(512)

        waitTD()
        taskDone = false
        BTManager.btGatt?.discoverServices()

        waitTD()
        val gattService = BTManager.btGatt?.getService(BTManager.serviceUUID)


        if(gattService == null)
        {
            Log.i("FAIL", "Gatt Service Null.")
            return ListenableWorker.Result.failure()
        }

        BTManager.connected = true

        //populates all BTInstances with their characteristic. characteristics r used to get set values etc etc.
        for(i in BTManager.btInstanceArray)
        {
            waitTD()
            i.characteristic = gattService.getCharacteristic(i.uuid)

            if(i.characteristic != null)
            {
                Log.i("info", "Discovered ${i.uuid}")
                if(i.enableNotification)
                {
                    Log.i("Info", "setting notification vars for ${i.uuid}")
                    BTManager.btGatt?.setCharacteristicNotification(i.characteristic, true)
                    val descriptor: BluetoothGattDescriptor = i.characteristic!!.getDescriptor(BTManager.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)

                    taskDone = false
                    BTManager.btGatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    waitTD()
                    Log.i("info", "succes nvset ${i.uuid}")
                }
            }

            else
            {
                Log.i("error", "Failed to find uuid ${i.uuid}")
            }

            //also execute the connected stuff
            if(i.bgCallback != null)
            {
                Log.i("info", "executing on connect for ${i.uuid}")
                i.bgCallback!!.onConnectionStateChange(BTManager.btGatt, BluetoothProfile.STATE_CONNECTED)
            }
        }

        return ListenableWorker.Result.success()
    }
}

class bCB : BluetoothGattCallback()
{
    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int)
    {
        Log.i("CS", "UPDATED")
        if(newState == BluetoothProfile.STATE_CONNECTED)
        {
            Log.i("Device status", "Connected!")
            BTWorker.taskDone = true
            BTManager.workManager?.enqueue(OneTimeWorkRequest.from(BTWorker::class.java))
        }

        else
        {
            Log.i("Device status", "$newState")
            BTManager.connected = false
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        BTManager.writeClean = true
        for(i in BTManager.btInstanceArray)
        {
            if(i.uuid == characteristic?.uuid)
            {
                i.writeClean = true
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {

        super.onMtuChanged(gatt, mtu, status)
        BTWorker.taskDone = true
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.i("Services", gatt?.services.toString())
        BTWorker.taskDone = true
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        Log.i("internal", "set d write")
        BTWorker.taskDone = true
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        Log.i("CC", "notify trig")
        for(i in BTManager.btInstanceArray)
        {
            if((i.uuid == characteristic.uuid))
            {
                if(i.bgCallback != null)
                {
                    i.characteristic?.let { i.bgCallback!!.onCharacteristicChange(it, value) }
                }
            }
        }
    }
}

class BTManager {
    companion object
    {
        val CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        var btGatt: BluetoothGatt? = null
        internal var connected = false
        internal var initSemaphore : Semaphore = Semaphore(1)
        internal var workManager : WorkManager? = null
        internal var btInstanceArray : Array<BTInstance> = emptyArray()
        internal var writeClean = true

        internal var serviceUUID : UUID = UUID.randomUUID()


        @SuppressLint("MissingPermission")
        public fun init(service: android.app.Service, btManager: BluetoothManager, su : UUID)
        {
            Log.i("Bluetooth Manager", "Bluetooth init...")
            this.serviceUUID = su

            BTWorker.taskDone = true
            val btAdapter = btManager.adapter
            val bd = btAdapter.getRemoteDevice("EC:62:60:32:C6:4E") as BluetoothDevice

            btGatt = bd.connectGatt(service as Context, true, bCB()) as BluetoothGatt
            workManager = WorkManager.getInstance(service as Context)

            Log.i("Bluetooth Manager", "Bluetooth init done!")
        }

        internal fun onconnectSetup()
        {

        }
    }
}