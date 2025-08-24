/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import kotlin.random.Random

/**
 * Jump Reset mode. A technique most players use to minimize the amount of knockback they get.
 */
internal object VelocityJumpReset : VelocityMode("JumpReset") {

    private val chance by float("Chance", 100f, 0f..100f, "%")

    private object JumpByReceivedHits : ToggleableConfigurable(ModuleVelocity, "JumpByReceivedHits", false) {
        val hitsUntilJump by intRange("HitsUntilJump", 2..2, 0..10)
    }

    private object JumpByDelay : ToggleableConfigurable(ModuleVelocity, "JumpByDelay", true) {
        val ticksUntilJump by intRange("UntilJump", 2..2, 0..20, "ticks")
    }

    init {
        tree(JumpByReceivedHits)
        tree(JumpByDelay)
    }

    private var limitUntilJump = 0
    private var isFallDamage = false

    private var hitsUntilJump = JumpByReceivedHits.hitsUntilJump.random()
    private var ticksUntilJump = JumpByDelay.ticksUntilJump.random()

    @Suppress("ComplexCondition", "unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        // To be able to alter velocity when receiving knockback, player must be sprinting.
        if (player.hurtTime != 9 || !player.isOnGround || !player.isSprinting ||
            isFallDamage || !isCooldownOver() || chance != 100f && Random.nextInt(100) > chance)
        {
            updateLimit()
            return@handler
        }

        event.jump = true
        limitUntilJump = 0

        hitsUntilJump = JumpByReceivedHits.hitsUntilJump.random()
        ticksUntilJump = JumpByDelay.ticksUntilJump.random()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            val velocityX = packet.velocityX / 8000.0
            val velocityY = packet.velocityY / 8000.0
            val velocityZ = packet.velocityZ / 8000.0

            // Check if the player is taking fall damage
            // We set this on every packet, because if the player gets hit afterward,
            // we will know that from the velocity.
            isFallDamage = velocityX == 0.0 && velocityZ == 0.0 && velocityY < 0
            ModuleDebug.debugParameter(this, "VelocityX", velocityX)
            ModuleDebug.debugParameter(this, "VelocityY", velocityY)
            ModuleDebug.debugParameter(this, "VelocityZ", velocityZ)
            ModuleDebug.debugParameter(this, "IsFallDamage", isFallDamage)
        }
    }

    private fun isCooldownOver(): Boolean {
        ModuleDebug.debugParameter(this, "HitsUntilJump", hitsUntilJump)
        ModuleDebug.debugParameter(this, "UntilJump", ticksUntilJump)

        return when {
            JumpByReceivedHits.enabled -> limitUntilJump >= hitsUntilJump
            JumpByDelay.enabled -> limitUntilJump >= ticksUntilJump
            else -> true // If none of the options are enabled, it will go automatic
        }
    }

    private fun updateLimit() {
        if (JumpByReceivedHits.enabled) {
            if (player.hurtTime == 9) {
                limitUntilJump++
            }
            return
        }

        limitUntilJump++
    }

}
