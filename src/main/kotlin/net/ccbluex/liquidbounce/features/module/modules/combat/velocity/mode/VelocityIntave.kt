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
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

object VelocityIntave : VelocityMode("Intave") {

    private class ReduceOnAttack(parent: EventListener?) : ToggleableConfigurable(
        parent, "ReduceOnAttack",
        true
    ) {
        private val reduceFactor by float("Factor", 0.6f, 0.6f..1f)
        private val hurtTime by intRange("HurtTime", 5..7, 1..10)
        private val lastAttackTimeToReduce by int("LastAttackTimeToReduce", 2000, 1..10000)
        var lastAttackTime = 0L

        @Suppress("unused")
        private val attackHandler = handler<AttackEntityEvent> { event ->
            if (event.isCancelled) {
                return@handler
            }

            if (player.hurtTime in hurtTime && System.currentTimeMillis() - lastAttackTime <= lastAttackTimeToReduce) {
                player.velocity.x *= reduceFactor
                player.velocity.z *= reduceFactor
            }
            lastAttackTime = System.currentTimeMillis()
        }
    }

    init {
        tree(ReduceOnAttack(this))
    }

    private class JumpReset(parent: EventListener?) : ToggleableConfigurable(
        parent, "JumpReset",
        true
    ) {

        private val chance by float("Chance", 50f, 0f..100f, "%")

        private inner class Randomize : ToggleableConfigurable(this, "Randomize", false) {
            val delayTicks by intRange("DelayTicks", 0..5, 0..10)
        }

        private val randomize = tree(Randomize())
        private var isFallDamage = false
        private var currentDelay = 0
        private var delayCounter = 0

        @Suppress("unused")
        private val tickJumpHandler = handler<MovementInputEvent> {
            val shouldJump = Math.random() * 100 < chance && player.hurtTime > 5 && !isFallDamage
            val canJump = player.isOnGround && mc.currentScreen !is InventoryScreen
            val shouldFinallyJump = shouldJump && canJump

            if (randomize.enabled) {
                delayCounter++

                if (delayCounter >= currentDelay) {
                    if (shouldFinallyJump) it.jump = true
                    delayCounter = 0
                    currentDelay = randomize.delayTicks.random()
                }
            } else {
                if (shouldFinallyJump) it.jump = true
            }
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

    }

    init {
        tree(JumpReset(this))
    }
}
