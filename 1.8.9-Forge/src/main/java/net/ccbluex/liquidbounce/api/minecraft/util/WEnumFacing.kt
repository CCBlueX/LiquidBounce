/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

enum class WEnumFacing(val directionVec: WVec3i, private val oppositeIndex: Int) {
    DOWN(WVec3i(0, -1, 0), 1),
    UP(WVec3i(0, 1, 0), 0),
    NORTH(WVec3i(0, 0, -1), 3),
    SOUTH(WVec3i(0, 0, 1), 2),
    WEST(WVec3i(-1, 0, 0), 5),
    EAST(WVec3i(1, 0, 0), 4);

    val opposite: WEnumFacing
        get() = values()[oppositeIndex]
}