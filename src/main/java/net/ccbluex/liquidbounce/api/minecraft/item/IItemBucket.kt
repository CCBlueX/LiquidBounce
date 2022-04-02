/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.item

import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock

interface IItemBucket : IItem {
    val isFull: IBlock
}