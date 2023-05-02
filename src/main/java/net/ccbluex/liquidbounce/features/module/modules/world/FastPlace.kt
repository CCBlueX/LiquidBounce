/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue

object FastPlace : Module("FastPlace", ModuleCategory.WORLD) {
    val speed by IntegerValue("Speed", 0, 0..4)
    val onlyBlocks by BoolValue("OnlyBlocks", true)
    val facingBlocks by BoolValue("OnlyWhenFacingBlocks", true)
}
