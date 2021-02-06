package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketClientStatus
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.injection.backend.Backend

fun createUseItemPacket(itemStack: IItemStack?, hand: WEnumHand): IPacket = if (Backend.MINECRAFT_VERSION_MINOR == 8) LiquidBounce.wrapper.classProvider.createCPacketPlayerBlockPlacement(itemStack) else LiquidBounce.wrapper.classProvider.createCPacketTryUseItem(hand)

fun createOpenInventoryPacket(): IPacket = if (Backend.MINECRAFT_VERSION_MINOR == 8) LiquidBounce.wrapper.classProvider.createCPacketClientStatus(ICPacketClientStatus.WEnumState.OPEN_INVENTORY_ACHIEVEMENT) else LiquidBounce.wrapper.classProvider.createCPacketEntityAction(LiquidBounce.wrapper.minecraft.thePlayer!!, ICPacketEntityAction.WAction.OPEN_INVENTORY)
