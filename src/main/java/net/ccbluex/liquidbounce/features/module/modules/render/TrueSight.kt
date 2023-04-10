/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.BoolValue

object TrueSight : Module("TrueSight", "Allows you to see invisible entities and barriers.", ModuleCategory.RENDER) {
    val barriersValue = BoolValue("Barriers", true)
    val entitiesValue = BoolValue("Entities", true)
}