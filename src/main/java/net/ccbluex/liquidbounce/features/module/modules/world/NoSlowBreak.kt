/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue

object NoSlowBreak : Module("NoSlowBreak", Category.WORLD, gameDetecting = false, hideModule = false) {
    val Blocks.AIR by BoolValue("Air", true)
    val water by BoolValue("Water", false)
}
