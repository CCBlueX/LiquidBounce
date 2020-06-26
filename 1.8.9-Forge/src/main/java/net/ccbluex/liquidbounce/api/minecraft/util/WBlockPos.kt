/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import kotlin.math.floor

class WBlockPos(x: Int, y: Int, z: Int) : WVec3i(x, y, z) {
    companion object {
        val ORIGIN: WBlockPos = WBlockPos(0, 0, 0)
    }

    constructor(x: Double, y: Double, z: Double) : this(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

    @JvmOverloads
    fun offset(side: WEnumFacing, n: Int = 1): WBlockPos {
        return if (n == 0) this else WBlockPos(x + side.directionVec.x * n, y + side.directionVec.y * n, z + side.directionVec.z * n)
    }

    fun up(): WBlockPos {
        return this.up(1)
    }

    fun up(n: Int): WBlockPos {
        return offset(WEnumFacing.UP, n)
    }

    fun down(): WBlockPos {
        return this.down(1)
    }

    fun down(n: Int): WBlockPos {
        return offset(WEnumFacing.DOWN, n)
    }
}