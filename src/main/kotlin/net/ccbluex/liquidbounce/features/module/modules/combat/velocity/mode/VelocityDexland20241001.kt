/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.utils.math.multiply

@Deprecated("Will be removed with the release of 0.43.0 and ported to LiquidBounce Legacy Add-on")
internal object VelocityDexland20241001 : VelocityMode("Dexland") {

    private val hReduce by float("HReduce", 0.3f, 0f..1f)
    private val times by int("AttacksToWork", 4, 1..10)

    private var lastAttackTime by clientState { 0L }
    var count by clientState { 0 }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (player.hurtTime > 0 && ++count % times == 0 && System.currentTimeMillis() - lastAttackTime <= 8000) {
            player.deltaMovement = player.deltaMovement.multiply(
                factorX = hReduce,
                factorZ = hReduce,
            )
        }

        lastAttackTime = System.currentTimeMillis()
    }

}
