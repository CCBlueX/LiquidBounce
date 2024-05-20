/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.BoolValue

object OverrideRaycast : Module("OverrideRaycast", Category.MISC, gameDetecting = false, hideModule = false) {
    private val alwaysActive by BoolValue("AlwaysActive", true)

    fun shouldOverride() = handleEvents() || alwaysActive
}