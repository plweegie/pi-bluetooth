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
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.DynamicSensorCallback
import android.os.Bundle
import android.util.Log
import org.eclipse.paho.client.mqttv3.*


class MainActivity : Activity(), MqttCallbackExtended, IMqttActionListener {

    companion object {
        private const val TEMP_SUBSCRIPTION_TOPIC = "sensor/temp"
        private const val PRESSURE_SUBSCRIPTION_TOPIC = "sensor/press"
    }

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var mSensorManager: SensorManager
    private lateinit var mqttHelper: MqttHelper

    private val mDynamicSensorCallback = object : DynamicSensorCallback() {
        override fun onDynamicSensorConnected(sensor: Sensor) {
            when (sensor.type) {
                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    Log.i(TAG, "Temperature sensor connected")
                    mTemperatureEventListener = TemperatureEventListener()
                    mSensorManager.registerListener(mTemperatureEventListener, sensor, 1000000)
                }
                Sensor.TYPE_PRESSURE -> {
                    Log.i(TAG, "Temperature sensor connected")
                    mPressureEventListener = PressureEventListener()
                    mSensorManager.registerListener(mPressureEventListener, sensor, 1000000)
                }
            }
        }
    }
    private lateinit var mTemperatureEventListener: TemperatureEventListener
    private lateinit var mPressureEventListener: PressureEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTemperaturePressureRequest()
        startMqtt()
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttHelper.disconnect()
        stopTemperaturePressureRequest()
    }

    private fun startMqtt() {
        mqttHelper = MqttHelper(applicationContext).apply {
            setMqttCallback(this@MainActivity)
            setPublishListener(this@MainActivity)
            connect()
        }
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

    private fun notifyTemperature(value: Float) {
        mqttHelper.publish(TEMP_SUBSCRIPTION_TOPIC, value.toString(), this)
    }

    private fun notifyPressure(value: Float) {
        mqttHelper.publish(PRESSURE_SUBSCRIPTION_TOPIC, value.toString(), this)
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        val toast = if (reconnect) "Reconnected" else "Connected"
        Log.d("MQTT", "$toast to $serverURI")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {

    }

    override fun connectionLost(cause: Throwable?) {
        Log.d("MQTT", "Connection lost")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d("MQTT", "Delivery complete: ${token?.message}")
    }

    override fun onSuccess(asyncActionToken: IMqttToken?) {
        Log.d("MQTT", "Publish successful")
    }

    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        Log.d("MQTT", "Publish failed")
    }

    private inner class TemperatureEventListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            Log.i(TAG, "Temperature changed: " + event.values[0])
            notifyTemperature(event.values[0])
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.i(TAG, "sensor accuracy changed: " + accuracy)
        }
    }

    private inner class PressureEventListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            Log.i(TAG, "Pressure changed: " + event.values[0])
            notifyPressure(event.values[0])
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.i(TAG, "sensor accuracy changed: " + accuracy)
        }
    }
}
