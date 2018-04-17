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

package com.plweegie.android.rpibleweather.gattserver

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.UUID
import kotlin.math.roundToInt


class SensorProfile {

    companion object {

        val ENVIRONMENTAL_SENSING_SERVICE = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")
        val TEMPERATURE_INFO = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")
        val PRESSURE_INFO = UUID.fromString("00002a6d-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        fun createSensorService(): BluetoothGattService {
            val service = BluetoothGattService(ENVIRONMENTAL_SENSING_SERVICE,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val temperatureInfo = BluetoothGattCharacteristic(TEMPERATURE_INFO,
                    BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ)
            val pressureInfo = BluetoothGattCharacteristic(PRESSURE_INFO,
                    BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ)

            val configDescriptor = BluetoothGattDescriptor(CLIENT_CONFIG,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)

            temperatureInfo.addDescriptor(configDescriptor)
            pressureInfo.addDescriptor(configDescriptor)

            service.apply {
                addCharacteristic(temperatureInfo)
                addCharacteristic(pressureInfo)
            }
            return service
        }

        fun getTemperature(temperature: Float): ByteArray {
            val temperatureAsInt = (100 * temperature).roundToInt()
            return byteArrayOf((temperatureAsInt shr 8).toByte(),
                    temperatureAsInt.toByte())
        }

        fun getPressure(pressure: Float): ByteArray {
            val pressureAsInt = (10 * pressure).roundToInt()
            return byteArrayOf((pressureAsInt ushr 24).toByte(),
                    (pressureAsInt ushr 16).toByte(),
                    (pressureAsInt ushr 8).toByte(),
                    pressureAsInt.toByte())
        }
    }
}