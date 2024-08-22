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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.clickScheduler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.utils.aiming.data.AngleLine
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.Entity

/**
 * A fight bot, fights for you, probably better than you. Lol.
 */
object FightBot : ToggleableConfigurable(ModuleKillAura, "FightBot", false) {

    private val safeRange by float("SafeRange", 4f, 0.1f..5f)
    private var sideToGo = false

    val repeatable = repeatable {
        sideToGo = !sideToGo

        waitTicks(
            if (player.horizontalCollision) {
                (60..90).random()
            } else {
                (10..35).random()
            }
        )
    }

    @Suppress("unused")
    val inputHandler = handler<MovementInputEvent>(priority = 1000) { ev ->
        val enemy = targetTracker.lockedOnTarget ?: return@handler
        val distance = enemy.boxedDistanceTo(player)

        if (clickScheduler.isClickOnNextTick()) {
            if (distance < ModuleKillAura.range) {
                ev.directionalInput = DirectionalInput.NONE
                sideToGo = !sideToGo
            } else {
                ev.directionalInput = DirectionalInput.FORWARDS
            }
        } else if (distance < safeRange) {
            ev.directionalInput = DirectionalInput.BACKWARDS
        } else {
            ev.directionalInput = DirectionalInput.NONE
        }

        // We are now in range of the player, so try to circle around him
        ev.directionalInput = ev.directionalInput.copy(left = !sideToGo, right = sideToGo)

        // Jump if we are stuck
        if (player.horizontalCollision) {
            ev.jumping = true
        }
    }

    private val maximumDriftRandom = (10f..30f).random()

    fun constructAngleLine(target: Entity): AngleLine? {
        if (!enabled) return null

        val targetDistance = target.boxedDistanceTo(player)

        // Unlikely that we can travel such distance with such basic methods
        if (targetDistance > 69) {
            return null
        }

        // Cause lag behind
        var box = target.box.center

        val positionNow = target.pos
        val prevTargetPosition = target.prevPos
        val diff = positionNow - prevTargetPosition

        box -= (diff * ((targetDistance - 4.0).coerceAtLeast(1.0)))

        val directRotation = AngleLine(toPoint = box)

        // todo: implement same-logic but with angle line
//        if (directRotation != player.rotation) {
//            val pitchDifference = abs(directRotation.pitch - player.rotation.pitch)
//
//            // Limit pitch difference
//            if (pitchDifference < maximumDriftRandom) {
//                directRotation.pitch = player.rotation.pitch + (-2f..2f).random().toFloat()
//            }
//        }

        // This is very basic and should be handled by the path finder in the future
        return directRotation
    }

}
