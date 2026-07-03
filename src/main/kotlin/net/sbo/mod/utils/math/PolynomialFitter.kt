package net.sbo.mod.utils.math

import kotlin.math.pow

class PolynomialFitter(private val degree: Int) {
    private val xPointMatrix = mutableListOf<List<Double>>()
    private val yPoints = mutableListOf<List<Double>>()

    fun addPoint(x: Double, y: Double) {
        this.yPoints.add(listOf(y))
        val xArray = MutableList(this.degree + 1) { 0.0 }
        for (i in xArray.indices) {
            xArray[i] = x.pow(i.toDouble())
        }
        this.xPointMatrix.add(xArray)
    }

    fun fit(): List<Double> {
        val xMatrix = Matrix(this.xPointMatrix)
        val yMatrix = Matrix(this.yPoints)

        val coeffsMatrix = xMatrix.transpose().multiply(xMatrix).inverse().multiply(xMatrix.transpose()).multiply(yMatrix)
        return coeffsMatrix.transpose().data[0]
    }
}