package com.plweegie.android.rpibleweather

import java.math.BigDecimal


fun Float.roundTo(decimalPlaces: Int): Float = BigDecimal(this.toDouble())
        .setScale(2, BigDecimal.ROUND_HALF_EVEN).toFloat()