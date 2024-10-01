/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.modes
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.pause

/**
 * Jump Reset mode. A technique most players use to minimize the amount of knockback they get.
 */
internal object VelocityJumpReset : Choice("JumpReset") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    object JumpByReceivedHits : ToggleableConfigurable(ModuleVelocity, "JumpByReceivedHits", false) {
        val hitsUntilJump by int("HitsUntilJump", 2, 0..10)
    }

    object JumpByDelay : ToggleableConfigurable(ModuleVelocity, "JumpByDelay", true) {
        val ticksUntilJump by int("UntilJump", 2, 0..20, "ticks")
    }

    init {
        tree(JumpByReceivedHits)
        tree(JumpByDelay)
    }

    var limitUntilJump = 0

    val tickJumpHandler = handler<MovementInputEvent> {
        // To be able to alter velocity when receiving knockback, player must be sprinting.
        if (player.hurtTime != 9 || !player.isOnGround || !player.isSprinting || !isCooldownOver()) {
            updateLimit()
            return@handler
        }

        it.jumping = true
        limitUntilJump = 0
    }

    fun isCooldownOver(): Boolean {
        return when {
            JumpByReceivedHits.enabled -> limitUntilJump >= JumpByReceivedHits.hitsUntilJump
            JumpByDelay.enabled -> limitUntilJump >= JumpByDelay.ticksUntilJump
            else -> true // If none of the options are enabled, it will go automatic
        }
    }

    fun updateLimit() {
        if (JumpByReceivedHits.enabled) {
            if (player.hurtTime == 9) {
                limitUntilJump++
            }
            return
        }

        limitUntilJump++
    }

    override fun handleEvents() = super.handleEvents() && pause == 0

}
