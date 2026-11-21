package net.sbo.mod.utils.math

import kotlin.math.pow
import kotlin.math.round
import java.text.DecimalFormat
import kotlin.math.abs

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

    private val NUMBER_FORMAT = DecimalFormat("#,###")
    private val BILLION_FORMAT = DecimalFormat("0.00b")
    private val MILLION_FORMAT = DecimalFormat("0.0m")
    private val THOUSAND_FORMAT = DecimalFormat("0.0k")
    private val INTEGER_FORMAT = DecimalFormat("0")

    fun formatNumber(number: Number?, withCommas: Boolean = false): String {
        val num = number?.toDouble() ?: 0.0

        return if (withCommas) {
            NUMBER_FORMAT.format(num)
        } else {
            when {
                abs(num) >= 1_000_000_000 -> BILLION_FORMAT.format(num / 1_000_000_000)
                abs(num) >= 1_000_000 -> MILLION_FORMAT.format(num / 1_000_000)
                abs(num) >= 1_000 -> THOUSAND_FORMAT.format(num / 1_000)
                else -> INTEGER_FORMAT.format(num)
            }
        }
    }
}
