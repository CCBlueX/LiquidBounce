/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

object ItemPhysics: Module("ItemPhysics", Category.RENDER, hideModule = false) {

    val realistic by BoolValue("Realistic", false)
    val weight by FloatValue("Weight", 0.5F, 0.1F..3F)
    val rotationSpeed by FloatValue("RotationSpeed", 1.0F, 0.01F..3F)

}