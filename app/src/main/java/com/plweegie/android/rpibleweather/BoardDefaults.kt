package com.plweegie.android.rpibleweather

import android.os.Build


class BoardDefaults {
    companion object {
        private const val DEVICE_RPI3 = "rpi3"
        private const val DEVICE_IMX6UL_PICO = "imx6ul_pico"
        private const val DEVICE_IMX7D_PICO = "imx7d_pico"

        fun getI2cBus(): String =
                when (Build.DEVICE) {
                    DEVICE_RPI3, DEVICE_IMX7D_PICO -> "I2C1"
                    DEVICE_IMX6UL_PICO -> "I2C2"
                    else -> {
                        throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
                    }
                }
    }
}