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

package net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.waterspeed

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.entity.movementForward
import net.ccbluex.liquidbounce.utils.entity.movementSideways
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

internal object WaterSpeedVanilla : ToggleableConfigurable(ModuleTerrainSpeed, "WaterSpeed", true) {

    val autoSwim by boolean("AutoSwimming", true)
    val speed by float("Speed", 0.1f, 0.01f..10f)
    object SwimmingBoost : ToggleableConfigurable(this@WaterSpeedVanilla, "SwimmingBoost", true) {
        val sprintBoost by float("Boost", 0.30f, 0.01f..10f)
    }

    init {
        tree(SwimmingBoost)
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        val player = mc.player ?: return@handler
        val forward = player.input.movementForward
        val strafe = player.input.movementSideways

        if(player.moving && player.isInWater) {
            val moveAngle = atan2(strafe.toDouble(), forward.toDouble())
            val finalYawRad = Math.toRadians(player.yRot.toDouble()) - moveAngle

            if (autoSwim) {
                player.isSprinting = true
                player.isSwimming = true
            }

            val speed = if(player.isSprinting && SwimmingBoost.enabled) {
                speed * (1.0 + SwimmingBoost.sprintBoost)
            } else {
                speed
            }

            player.deltaMovement = Vec3(
                -(finalYawRad).fastSin().toDouble() * speed.toDouble(),
                player.deltaMovement.y, (finalYawRad).fastCos().toDouble() * speed.toDouble()
            )
        }
        if(mc.options.keyJump.isDown && player.isInWater) {
            player.deltaMovement = Vec3(player.deltaMovement.x, 0.25, player.deltaMovement.z)
        }
        if(mc.options.keyShift.isDown && player.isInWater) {
            player.deltaMovement = Vec3(player.deltaMovement.x, -0.25, player.deltaMovement.z)
        }
    }
}
