package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.equipment

import net.ccbluex.liquidbounce.api.enums.WEnumEquipmentSlot
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntityEquipment
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class EquipmentChangeFrequencyCheck : BotCheck("status.equipment.changeFrequency")
{
	override val isActive: Boolean
		get() = AntiBot.equipmentChangeFrequencyEnabledValue.get()

	private val previousEquipmentPacketMap = mutableMapOf<Int, MutableMap<WEnumEquipmentSlot, ISPacketEntityEquipment>>()
	private val previousEquipmentDelayMap = mutableMapOf<Int, MutableMap<WEnumEquipmentSlot, MSTimer>>()
	private val overallDelayMap = mutableMapOf<Int, MSTimer>()
	private val invalidPacket = mutableSetOf<Int>()
	private val vl = mutableMapOf<Int, Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean
	{
		val entityId = target.entityId
		return entityId in invalidPacket || (vl[entityId] ?: 0) >= AntiBot.equipmentChangeFrequencyVLLimitValue.get()
	}

	override fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isSPacketEntityEquipment(packet))
		{
			val theWorld = mc.theWorld ?: return

			val equipmentPacket = packet.asSPacketEntityEquipment()

			val entityId = equipmentPacket.entityID
			val slot = equipmentPacket.slot
			val item = equipmentPacket.item

			val entity = theWorld.getEntityByID(entityId)

			if (entity != null && classProvider.isEntityPlayer(entity))
			{
				val target = entity.asEntityPlayer()

				var vlIncrement = 0

				val prev = previousEquipmentPacketMap[entityId]?.get(slot)
				val perSlotDelay = previousEquipmentDelayMap[entityId]?.get(slot)
				val overallDelay = overallDelayMap[entityId]
				if (prev != null)
				{
					if (prev.item == item)
					{
						invalidPacket += entityId
						notification(target) { arrayOf("reason=(actual content is same)") }
					}

					val perSlotDelayLimit = AntiBot.equipmentChangeFrequencyPerSlotDelayValue.get().toLong()
					if (perSlotDelay != null && !perSlotDelay.hasTimePassed(perSlotDelayLimit))
					{
						vlIncrement = ((perSlotDelayLimit - perSlotDelay.getTime()).toInt() / 50).coerceAtLeast(3)
						notification(target) { arrayOf("reason=frequency (per slot)", "frequency=${perSlotDelay.getTime()}") }
					}

					val overallDelayLimit = AntiBot.equipmentChangeFrequencyOverallDelayValue.get().toLong()
					if (overallDelay != null && !overallDelay.hasTimePassed(overallDelayLimit))
					{
						vlIncrement = ((overallDelayLimit - overallDelay.getTime()).toInt() / 50).coerceAtLeast(5)
						notification(target) { arrayOf("reason=frequency (all)", "frequency=${overallDelay.getTime()}") }
					}
				}
				previousEquipmentPacketMap.computeIfAbsent(entityId) { mutableMapOf() }[slot] = equipmentPacket
				previousEquipmentDelayMap.computeIfAbsent(entityId) { mutableMapOf() }[slot] = MSTimer().apply(MSTimer::reset)
				overallDelayMap.computeIfAbsent(entityId) { MSTimer() }.reset()

				val previousVL = vl[entityId] ?: 0
				if (vlIncrement > 0) vl[entityId] = previousVL + vlIncrement
				else if (AntiBot.equipmentChangeFrequencyVLDecValue.get())
				{
					val currentVL = previousVL - 1
					if (currentVL <= 0) vl.remove(entityId) else vl[entityId] = currentVL
				}
			}
		}
	}

	override fun clear()
	{
		previousEquipmentPacketMap.clear()
		previousEquipmentDelayMap.clear()
		overallDelayMap.clear()
		invalidPacket.clear()
		vl.clear()
	}
}
