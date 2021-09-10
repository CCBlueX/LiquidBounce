package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

// TODO: Adjustable helmet/chestplate/leggings/boots values
class EquipmentCheck : BotCheck("misc.equipment")
{
	override val isActive: Boolean
		get() = AntiBot.armorValue.get()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer) = target.inventory.armorInventory.all { it == null }
}
