/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.BoolValue

object NoSlowBreak : Module("NoSlowBreak", "Automatically adjusts breaking speed when using modules that influence it.", ModuleCategory.WORLD) {
    val airValue = BoolValue("Air", true)
    val waterValue = BoolValue("Water", false)
}
