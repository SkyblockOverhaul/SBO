package net.sbo.mod.utils.math

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.sbo.mod.SBOKotlin
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A 3D vector class for mathematical operations and Minecraft world interactions.
 * The credits to this class go fully to the Skyhanni Mod: https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/utils/LorenzVec.kt
 */
data class SboVec(var x: Double, var y: Double, var z: Double) {

    fun distanceTo(other: SboVec): Double {
        return sqrt((other.x - this.x).pow(2) + (other.y - this.y).pow(2) + (other.z - this.z).pow(2))
    }

    fun distanceTo(x: Double, y: Double, z: Double): Double {
        return sqrt((x - this.x).pow(2) + (y - this.y).pow(2) + (z - this.z).pow(2))
    }

    operator fun plus(other: SboVec): SboVec {
        return SboVec(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    operator fun minus(other: SboVec): SboVec {
        return SboVec(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    operator fun times(d: Double): SboVec {
        return SboVec(this.x * d, this.y * d, this.z * d)
    }

    fun clone(): SboVec = this.copy()

    fun down(amount: Double = 1.0): SboVec {
        return this.copy(y = this.y - amount)
    }

    fun up(amount: Double = 1.0): SboVec {
        return this.copy(y = this.y + amount)
    }

    fun add(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): SboVec =
        SboVec(this.x + x, this.y + y, this.z + z)

    fun add(x: Int = 0, y: Int = 0, z: Int = 0): SboVec = SboVec(this.x + x, this.y + y, this.z + z)


    fun roundLocationToBlock(): SboVec {
        return SboVec(floor(this.x), floor(this.y), floor(this.z))
    }

    fun toVec3d(): Vec3 {
        return Vec3(this.x, this.y, this.z)
    }

    fun center(): SboVec {
        return SboVec(this.x + 0.5, this.y + 0.5, this.z + 0.5)
    }

    fun toCleanString(): String {
        return "%.2f, %.2f, %.2f".format(this.x, this.y, this.z)
    }

    fun toDoubleArray(): DoubleArray {
        return doubleArrayOf(this.x, this.y, this.z)
    }

    fun length(): Double {
        return sqrt(x * x + y * y + z * z)
    }

    fun roundToBlock() = SboVec(floor(x), floor(y), floor(z))

    fun normalize() = length().let { SboVec(x / it, y / it, z / it) }

    fun lengthSquared(): Double = x * x + y * y + z * z

    fun isNormalized(tolerance: Double = 0.01) = (lengthSquared() - 1.0).absoluteValue < tolerance

    fun crossProduct(other: SboVec): SboVec = SboVec(
        this.y * other.z - this.z * other.y,
        this.z * other.x - this.x * other.z,
        this.x * other.y - this.y * other.x,
    )

    fun distance(other: SboVec): Double = distanceSq(other).pow(0.5)

    fun distanceSq(other: SboVec): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return (dx * dx + dy * dy + dz * dz)
    }

    fun isInLoadedChunk(): Boolean = SBOKotlin.mc.level?.isLoaded(toBlockPos()) ?: false

    fun toBlockPos(): BlockPos = BlockPos(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

    fun getBlockAt(): Block? = getBlockStateAt()?.block

    fun getBlockStateAt(): BlockState? = SBOKotlin.mc.level?.getBlockState(toBlockPos())

    fun dotProduct(other: SboVec): Double = (x * other.x) + (y * other.y) + (z * other.z)

    fun scale(scalar: Double): SboVec = SboVec(scalar * x, scalar * y, scalar * z)

    companion object {
        val ZERO: SboVec = BlockPos.ZERO.toSboVec()

        fun fromArray(arr: List<Double>): SboVec {
            require(arr.size >= 3) { "Array must contain at least 3 elements for x, y, z." }
            return SboVec(arr[0], arr[1], arr[2])
        }

        fun Vec3.toSboVec(): SboVec {
            return SboVec(this.x, this.y, this.z)
        }

        fun BlockPos.toSboVec(): SboVec {
            return SboVec(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
        }
    }
}
