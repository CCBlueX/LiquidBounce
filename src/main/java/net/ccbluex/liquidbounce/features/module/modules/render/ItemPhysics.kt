/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.FloatValue

object ItemPhysics: Module("ItemPhysics", ModuleCategory.RENDER) {

    val weight = FloatValue("Weight", 0.5F, 0.1F..3F)
    val rotationSpeed = FloatValue("RotationSpeed", 1.0F, 0.01F..3F)

}