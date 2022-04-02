package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketClientStatus
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider

inline fun createUseItemPacket(itemStack: IItemStack?, hand: WEnumHand): IPacket {
    @Suppress("ConstantConditionIf")
    return if (Backend.MINECRAFT_VERSION_MINOR == 8) {
        classProvider.createCPacketPlayerBlockPlacement(itemStack)
    } else {
        classProvider.createCPacketTryUseItem(hand)
    }
}

inline fun createOpenInventoryPacket(): IPacket {
    @Suppress("ConstantConditionIf")
    return if (Backend.MINECRAFT_VERSION_MINOR == 8) {
        classProvider.createCPacketClientStatus(ICPacketClientStatus.WEnumState.OPEN_INVENTORY_ACHIEVEMENT)
    } else {
        classProvider.createCPacketEntityAction(LiquidBounce.wrapper.minecraft.thePlayer!!, ICPacketEntityAction.WAction.OPEN_INVENTORY)
    }
}
