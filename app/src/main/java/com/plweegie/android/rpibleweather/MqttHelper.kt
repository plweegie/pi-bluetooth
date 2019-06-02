package com.plweegie.android.rpibleweather

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MqttHelper(private val context: Context) {

    companion object {
        private const val SERVER_URI = "tcp://m24.cloudmqtt.com:18272"
        private const val CLIENT_ID = "MqttAndroidBoss"
    }

    private var mqttAndroidClient: MqttAndroidClient? = null
    private var mqttCallback: MqttCallbackExtended? = null
    private var publishListener: IMqttActionListener? = null

    private val mqttConnectOptions = MqttConnectOptions().apply {
        isAutomaticReconnect = true
        isCleanSession = false
        connectionTimeout = 30
        keepAliveInterval = 60
        userName = context.resources.getString(R.string.user_name)
        password = context.resources.getString(R.string.user_password).toCharArray()
    }

    private val disconnectedBufferOptions = DisconnectedBufferOptions().apply {
        isBufferEnabled = true
        bufferSize = 100
        isPersistBuffer = false
        isDeleteOldestMessages = false
    }

    private val connectListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            Log.d("MQTT", "Connected")
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            Log.d("MQTT", "Error connecting to MQTT")
        }
    }

    fun setMqttCallback(callback: MqttCallbackExtended) {
        mqttCallback = callback
    }

    fun setPublishListener(listener: IMqttActionListener) {
        publishListener = listener
    }

    fun connect() {
        if (isConnected()) {
            return
        }

        try {
            mqttAndroidClient = MqttAndroidClient(context, SERVER_URI, CLIENT_ID).apply {
                setCallback(mqttCallback)
                setBufferOpts(disconnectedBufferOptions)
                connect(mqttConnectOptions, context, connectListener)
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        if (!isConnected()) {
            return
        }

        try {
            mqttAndroidClient?.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String, listener: IMqttActionListener) {
        if (!isConnected()) {
            return
        }

        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttAndroidClient?.publish(topic, mqttMessage, context, listener)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun isConnected(): Boolean = (mqttAndroidClient != null && mqttAndroidClient?.isConnected == true)
}