package net.ccbluex.liquidbounce.utils.math

import kotlin.math.sqrt

open class Vec2i(val x: Int, val y: Int) {
    operator fun component1() = x
    operator fun component2() = y
    fun add(vec: Vec2i): Vec2i {
        return Vec2i(this.x + vec.x, this.y + vec.y)
    }

    fun dotProduct(otherVec: Vec2i): Int {
        return this.x * otherVec.x + this.y * otherVec.y
    }

    fun similarity(otherVec: Vec2i): Double {
        return this.dotProduct(otherVec) / sqrt(this.lengthSquared().toDouble() * otherVec.lengthSquared())
    }

    fun length(): Double {
        return sqrt(lengthSquared().toDouble())
    }

    private fun lengthSquared(): Int {
        return this.x * this.x + this.y * this.y
    }
}
