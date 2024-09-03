package com.example.btmodule

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.sync.Semaphore


class BTWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams)
{
    override fun doWork(): Result
    {
        Log.i("BTWorker", "Start")
        return ListenableWorker.Result.success()
    }
}

class bCB : BluetoothGattCallback()
{
    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int)
    {
        if(newState == BluetoothProfile.STATE_CONNECTED)
        {
            Log.i("Device status", "Connected!")
            gatt?.requestMtu(512)
            BTManager.workManager?.enqueue(OneTimeWorkRequest.from(BTWorker::class.java))
        }

        else
        {
            Log.i("Device status", "$newState")
            BTManager.connected = false
        }

        super.onConnectionStateChange(gatt, status, newState)
    }
}

class BTManager {
    companion object
    {
        var btGatt: BluetoothGatt? = null
        internal var connected = false
        internal var initSemaphore : Semaphore = Semaphore(1)
        internal var workManager : WorkManager? = null

        @SuppressLint("MissingPermission")
        public fun init(service: android.app.Service, btManager: BluetoothManager, btInstances: () -> BTInstance)
        {
            Log.i("Bluetooth Manager", "Bluetooth init...")
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