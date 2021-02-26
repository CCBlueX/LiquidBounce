/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import java.util.*

@ModuleInfo(name = "TrueSight", description = "Allows you to see invisible entities and barriers.", category = ModuleCategory.RENDER)
class TrueSight : Module()
{
	val barriersValue = BoolValue("Barriers", true)
	val entitiesValue = BoolValue("Entities", true)
	val entitiesAlphaValue = FloatValue("EntitiesAlpha", 0.15F, 0.05F, 0.5F)

	override val tag: String
		get()
		{
			val tagBuilder = StringJoiner(" and ")
			if (barriersValue.get()) tagBuilder.add("Barriers")
			if (entitiesValue.get()) tagBuilder.add("Entities")

			return "$tagBuilder"
		}
}
