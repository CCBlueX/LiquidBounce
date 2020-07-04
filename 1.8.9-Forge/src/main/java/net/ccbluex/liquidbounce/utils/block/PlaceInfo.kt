/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider


class PlaceInfo(val blockPos: WBlockPos, val enumFacing: IEnumFacing,
                var vec3: WVec3 = WVec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)) {

    companion object {

        /**
         * Allows you to find a specific place info for your [blockPos]
         */
        @JvmStatic
        fun get(blockPos: WBlockPos): PlaceInfo? {
            return when {
                BlockUtils.canBeClicked(blockPos.add(0, -1, 0)) ->
                    return PlaceInfo(blockPos.add(0, -1, 0), classProvider.getEnumFacing(EnumFacingType.UP))
                BlockUtils.canBeClicked(blockPos.add(0, 0, 1)) ->
                    return PlaceInfo(blockPos.add(0, 0, 1), classProvider.getEnumFacing(EnumFacingType.NORTH))
                BlockUtils.canBeClicked(blockPos.add(-1, 0, 0)) ->
                    return PlaceInfo(blockPos.add(-1, 0, 0), classProvider.getEnumFacing(EnumFacingType.EAST))
                BlockUtils.canBeClicked(blockPos.add(0, 0, -1)) ->
                    return PlaceInfo(blockPos.add(0, 0, -1), classProvider.getEnumFacing(EnumFacingType.SOUTH))
                BlockUtils.canBeClicked(blockPos.add(1, 0, 0)) ->
                    PlaceInfo(blockPos.add(1, 0, 0), classProvider.getEnumFacing(EnumFacingType.WEST))
                else -> null
            }
        }

    }
}