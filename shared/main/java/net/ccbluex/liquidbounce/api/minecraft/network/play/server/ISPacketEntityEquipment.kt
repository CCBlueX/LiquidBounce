/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.enums.WEnumEquipmentSlot

interface ISPacketEntityEquipment
{
    val entityID: Int
    val slot: WEnumEquipmentSlot
    val item: IItemStack?
}
