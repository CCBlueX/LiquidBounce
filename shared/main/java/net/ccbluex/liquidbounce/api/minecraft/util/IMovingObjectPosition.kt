/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity

interface IMovingObjectPosition
{
    val hitVec: WVec3
    val typeOfHit: WMovingObjectType

    val entityHit: IEntity?

    val blockPos: WBlockPos?
    val sideHit: IEnumFacing?

    enum class WMovingObjectType
    {
        MISS,
        ENTITY,
        BLOCK
    }
}
