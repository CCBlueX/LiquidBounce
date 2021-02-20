package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketClientStatus
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.injection.backend.Backend

fun createUseItemPacket(itemStack: IItemStack?, hand: WEnumHand): IPacket
{
	val classProvider = wrapper.classProvider
	return if (Backend.MINECRAFT_VERSION_MINOR == 8) classProvider.createCPacketPlayerBlockPlacement(itemStack) else classProvider.createCPacketTryUseItem(hand)
}

fun createOpenInventoryPacket(): IPacket
{
	val classProvider = wrapper.classProvider
	return if (Backend.MINECRAFT_VERSION_MINOR == 8) classProvider.createCPacketClientStatus(ICPacketClientStatus.WEnumState.OPEN_INVENTORY_ACHIEVEMENT) else classProvider.createCPacketEntityAction(wrapper.minecraft.thePlayer!!, ICPacketEntityAction.WAction.OPEN_INVENTORY)
}
