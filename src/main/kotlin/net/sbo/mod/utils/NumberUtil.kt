package net.sbo.mod.utils

import kotlin.math.pow
import kotlin.math.round

object NumberUtil {
    /**
     * This code was unmodified and taken under CC BY-SA 3.0 license
     * @link https://stackoverflow.com/a/22186845
     * @author jpdymond
     */
    fun Double.roundTo(precision: Int): Double {
        val scale = 10.0.pow(precision)
        return round(this * scale) / scale
    }

    fun Float.roundTo(precision: Int): Float = toDouble().roundTo(precision).toFloat()
}
