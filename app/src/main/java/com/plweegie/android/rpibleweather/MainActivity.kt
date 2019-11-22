/* Copyright 2018 Jan K Szymanski. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

==============================================================================*/

package com.plweegie.android.rpibleweather

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.DynamicSensorCallback
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import com.plweegie.android.rpibleweather.gattserver.SensorProfile
import java.util.*

private val TAG = MainActivity::class.java.simpleName

class MainActivity : Activity() {
    private lateinit var mSensorManager: SensorManager
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mAdvertiser: BluetoothLeAdvertiser

    private var mBluetoothGattServer: BluetoothGattServer? = null
    private val mBleDevices: MutableSet<BluetoothDevice> = mutableSetOf()

    private val mDynamicSensorCallback = object : DynamicSensorCallback() {
        override fun onDynamicSensorConnected(sensor: Sensor) {
            when (sensor.type) {
                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    Log.i(TAG, "Temperature sensor connected")
                    mTemperatureEventListener = TemperatureEventListener()
                    mSensorManager.registerListener(mTemperatureEventListener, sensor, 5000000)
                }
                Sensor.TYPE_PRESSURE -> {
                    Log.i(TAG, "Temperature sensor connected")
                    mPressureEventListener = PressureEventListener()
                    mSensorManager.registerListener(mPressureEventListener, sensor, 5000000)
                }
            }
        }
    }
    private lateinit var mTemperatureEventListener: TemperatureEventListener
    private lateinit var mPressureEventListener: PressureEventListener

    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode)
        }
    }

    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "BluetoothDevice CONNECTED: " + device)
                }
                BluetoothProfile.STATE_DISCONNECTED -> mBleDevices.remove(device)
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic?) {

            when (characteristic?.uuid) {
                SensorProfile.TEMPERATURE_INFO, SensorProfile.PRESSURE_INFO -> {
                    if (characteristic != null) {
                        mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                                0, characteristic.value)
                    }
                }
                else -> {
                    mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE,
                            0, null)
                }
            }

        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor) {
            if (descriptor.uuid == SensorProfile.CLIENT_CONFIG) {
                val returnValue = if (mBleDevices.contains(device)) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }

                mBluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue)
            } else {
                mBluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0, null)
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean,
                                              responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            if (descriptor?.uuid == SensorProfile.CLIENT_CONFIG) {
                if (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE contentEquals (value as ByteArray) ) {
                    mBleDevices.add(device)
                } else if (BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE contentEquals value) {
                    mBleDevices.remove(device)
                }

                if (responseNeeded) {
                    mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                            0, null)
                }
            } else {
                if (responseNeeded) {
                    mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE,
                            0, null)
                }
            }
        }
    }

    private val mBluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    startAdvertising()
                    startServer()
                }
                BluetoothAdapter.STATE_OFF -> {
                    stopServer()
                    stopAdvertising()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTemperaturePressureRequest()

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = mBluetoothManager.adapter

        if (bluetoothAdapter == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
        }

        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBluetoothReceiver, intentFilter)

        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services")
            startAdvertising()
            startServer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTemperaturePressureRequest()

        if (mBluetoothManager.adapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }

        unregisterReceiver(mBluetoothReceiver)
    }

    private fun startTemperaturePressureRequest() {
        this.startService(Intent(this, TemperaturePressureService::class.java))
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback)
    }

    private fun stopTemperaturePressureRequest() {
        this.stopService(Intent(this, TemperaturePressureService::class.java))
        mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback)
        mSensorManager.apply {
            unregisterListener(mTemperatureEventListener)
            unregisterListener(mPressureEventListener)
        }
    }

    private fun startAdvertising() {
        val adapter = mBluetoothManager.adapter
        mAdvertiser = adapter.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

        val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(SensorProfile.ENVIRONMENTAL_SENSING_SERVICE))
                .build()

        mAdvertiser.startAdvertising(settings, data, mAdvertiseCallback)
    }

    private fun stopAdvertising() {
        mAdvertiser.stopAdvertising(mAdvertiseCallback)
    }

    private fun startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback)
        mBluetoothGattServer?.addService(SensorProfile.createSensorService())
    }

    private fun stopServer() {
        mBluetoothGattServer?.close()
    }

    private fun notifyEnvironmentalParam(value: Int, characteristicUUID: UUID) {
        if (mBleDevices.isEmpty()) {
            return
        }

        for (device in mBleDevices) {
            val characteristic = mBluetoothGattServer
                    ?.getService(SensorProfile.ENVIRONMENTAL_SENSING_SERVICE)
                    ?.getCharacteristic(characteristicUUID)
            

            val valueFormat = if (characteristicUUID == SensorProfile.TEMPERATURE_INFO) BluetoothGattCharacteristic.FORMAT_SINT16
                else BluetoothGattCharacteristic.FORMAT_UINT32
            characteristic?.setValue(value, valueFormat, 0)
            mBluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private inner class TemperatureEventListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            Log.i(TAG, "Temperature changed: " + event.values[0])
            notifyEnvironmentalParam(SensorProfile.getTemperature(event.values[0]), SensorProfile.TEMPERATURE_INFO)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.i(TAG, "sensor accuracy changed: " + accuracy)
        }
    }

    private inner class PressureEventListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            Log.i(TAG, "Pressure changed: " + event.values[0])
            notifyEnvironmentalParam(SensorProfile.getPressure(event.values[0]), SensorProfile.PRESSURE_INFO)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.i(TAG, "sensor accuracy changed: " + accuracy)
        }
    }
}
