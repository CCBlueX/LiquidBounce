/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue

object Chams : Module("Chams", Category.RENDER, hideModule = false) {
    val targets by BoolValue("Targets", true)
    val chests by BoolValue("Chests", true)
    val items by BoolValue("Items", true)
}
