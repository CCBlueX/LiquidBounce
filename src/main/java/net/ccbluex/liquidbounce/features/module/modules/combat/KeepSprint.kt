/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.FloatValue

object KeepSprint : Module("KeepSprint", Category.COMBAT, hideModule = false) {
    val motionAfterAttackOnGround by FloatValue("MotionAfterAttackOnGround", 0.6f, 0.0f..1f)
    val motionAfterAttackInAir by FloatValue("MotionAfterAttackInAir", 0.6f, 0.0f..1f)

    val motionAfterAttack
        get() = if (mc.player.onGround) motionAfterAttackOnGround else motionAfterAttackInAir
}