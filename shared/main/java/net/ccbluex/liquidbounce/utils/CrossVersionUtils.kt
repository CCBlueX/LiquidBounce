package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketClientStatus
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.injection.backend.Backend

fun createUseItemPacket(itemStack: IItemStack?, hand: WEnumHand): IPacket = if (Backend.MINECRAFT_VERSION_MINOR == 8) wrapper.classProvider.createCPacketPlayerBlockPlacement(itemStack) else wrapper.classProvider.createCPacketTryUseItem(hand)

fun isOpenInventoryPacket(packet: IPacket): Boolean = if (Backend.MINECRAFT_VERSION_MINOR == 8) wrapper.classProvider.isCPacketClientStatus(packet) && packet.asCPacketClientStatus().status == ICPacketClientStatus.WEnumState.OPEN_INVENTORY_ACHIEVEMENT else wrapper.classProvider.isCPacketEntityAction(packet) && packet.asCPacketEntityAction().action == ICPacketEntityAction.WAction.OPEN_INVENTORY

fun createOpenInventoryPacket(): IPacket = if (Backend.MINECRAFT_VERSION_MINOR == 8) wrapper.classProvider.createCPacketClientStatus(ICPacketClientStatus.WEnumState.OPEN_INVENTORY_ACHIEVEMENT) else wrapper.classProvider.createCPacketEntityAction(wrapper.minecraft.thePlayer!!, ICPacketEntityAction.WAction.OPEN_INVENTORY)
