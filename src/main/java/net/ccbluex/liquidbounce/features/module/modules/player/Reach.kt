/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.max

object Reach : Module("Reach", ModuleCategory.PLAYER) {

    val combatReach by FloatValue("CombatReach", 3.5f, 3f..7f)
    val buildReach by FloatValue("BuildReach", 5f, 4.5f..7f)

    val maxRange
        get() = max(combatReach, buildReach)
}
