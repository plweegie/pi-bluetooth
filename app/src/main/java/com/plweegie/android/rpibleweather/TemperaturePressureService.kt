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

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver

import java.io.IOException


/**
 * To use this service, start it from your component (like an activity):
 * <pre>{@code
 * this.startService(Intent(this, TemperaturePressureService::class.java))
 * }</pre>
 */
class TemperaturePressureService : Service() {

    private val TAG = TemperaturePressureService::class.java.simpleName

    private lateinit var mTemperatureSensorDriver: Bmx280SensorDriver

    override fun onCreate() {
        setupTemperaturePressureSensor()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyTemperaturePressureSensor()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }

    private fun setupTemperaturePressureSensor() {
        try {
            mTemperatureSensorDriver = Bmx280SensorDriver(BoardDefaults.getI2cBus())
            mTemperatureSensorDriver.apply {
                registerTemperatureSensor()
                registerPressureSensor()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error configuring sensor", e)
        }
    }

    private fun destroyTemperaturePressureSensor() {
        mTemperatureSensorDriver.apply {
            unregisterTemperatureSensor()
            unregisterPressureSensor()
        }
        try {
            mTemperatureSensorDriver.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing sensor", e)
        }
    }
}
