package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.render.ColorUtils

class DuplicateInWorldExistenceCheck : BotCheck("tab.duplicateInWorld.existence")
{
	override val isActive: Boolean
		get() = AntiBot.duplicateInWorldExistenceEnabledValue.get()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean
	{
		val stripColors = AntiBot.duplicateInWorldExistenceStripColorsValue.get()
		val tryStripColors = { string: String -> if (stripColors) ColorUtils.stripColor(string) else string }

		val mode = AntiBot.duplicateInWorldExistenceNameModeValue.get().toLowerCase()
		val entityName = when (mode)
		{
			"displayname" -> target.displayName.formattedText
			"customnametag" -> target.customNameTag.ifBlank { target.gameProfile.name }
			else -> target.gameProfile.name
		}?.let(tryStripColors)

		return AntiBot.duplicateInWorldExistenceEnabledValue.get() && theWorld.loadedEntityList.filter(classProvider::isEntityPlayer).map(IEntity::asEntityPlayer).count {
			entityName == when (mode)
			{
				"displayname" -> it.displayName.formattedText
				"customnametag" -> it.customNameTag.ifBlank { it.gameProfile.name }
				else -> it.gameProfile.name
			}
		} > 1
	}
}
