/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "Teams", description = "Prevents KillAura from attacking team mates.", category = ModuleCategory.MISC)
class Teams : Module()
{
	private val scoreboardValue = BoolValue("ScoreboardTeam", true)
	private val colorValue = BoolValue("Color", true)
	private val gommeSWValue = BoolValue("GommeSW", false)
	// TODO: private val armorColorValue = BoolValue("ArmorColor", false)

	/**
	 * Check if [entity] is in your own team using scoreboard, name color or team prefix
	 */
	fun isInYourTeam(entity: IEntityLivingBase): Boolean
	{
		val thePlayer = mc.thePlayer ?: return false

		val playerTeam = thePlayer.team
		val targetTeam = entity.team

		// Scoreboard Team
		if (scoreboardValue.get() && playerTeam != null && targetTeam != null && playerTeam.isSameTeam(targetTeam)) return true

		val displayName = thePlayer.displayName.formattedText
		val entityDisplayName = entity.displayName.formattedText

		// GommeSkywars
		if (gommeSWValue.get())
		{
			val targetName = entityDisplayName.replace("\u00A7r", "")
			val clientName = displayName.replace("\u00A7r", "")

			if (targetName.startsWith("T") && clientName.startsWith("T")) if (targetName[1].isDigit() && clientName[1].isDigit()) return targetName[1] == clientName[1]
		}

		// Color
		if (colorValue.get())
		{
			val targetName = entityDisplayName.replace("\u00A7r", "")
			val clientName = displayName.replace("\u00A7r", "")

			return targetName.startsWith("\u00A7${clientName[1]}")
		}

		return false
	}
}
