package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.enums.WEnumEquipmentSlot
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntityEquipment
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.server.SPacketEntityEquipment

class SPacketEntityEquipmentImpl<out T : SPacketEntityEquipment>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntityEquipment
{
	override val entityID: Int
		get() = wrapped.entityID

	override val slot: WEnumEquipmentSlot
		get() = wrapped.equipmentSlot.wrap()

	override val item: IItemStack?
		get() = wrapped.itemStack?.wrap()
}

fun ISPacketEntityEquipment.unwrap(): SPacketEntityEquipment = (this as SPacketEntityEquipmentImpl<*>).wrapped
fun SPacketEntityEquipment.wrap(): ISPacketEntityEquipment = SPacketEntityEquipmentImpl(this)
