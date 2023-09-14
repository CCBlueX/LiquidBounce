/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import kotlin.random.Random

/**
 * SuperKnockback module
 *
 * Increases knockback dealt to other entities.
 */
object ModuleSuperKnockback : Module("SuperKnockback", Category.COMBAT) {

    val modes = choices("Mode", Packet, arrayOf(Packet, SprintTap, WTap))
    val hurtTime by int("HurtTime", 10, 0..10)
    val chance by int("Chance", 100, 0..100)

    object Packet : Choice("Packet") {
        override val parent: ChoiceConfigurable
            get() = modes

        val attackHandler = handler<AttackEvent> { event ->
            val enemy = event.enemy

            if (enemy is LivingEntity && enemy.hurtTime <= hurtTime && chance > Random.nextInt(
                    0, 99
                ) && !ModuleCriticals.wouldCrit()
            ) {
                if (player.isSprinting) {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                }

                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING))
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING))

                player.isSprinting = true
                player.lastSprinting = true
            }
        }
    }

    object SprintTap : Choice("SprintTap") {
        override val parent: ChoiceConfigurable
            get() = modes

        val reSprintTicks by intRange("ReSprintTicks", 0..1, 0..10)

        var antiSprint = false

        override fun enable() {
            antiSprint = false
        }

        val attackHandler = sequenceHandler<AttackEvent> { event ->
            if (!shouldStopSprinting(event)) {
                return@sequenceHandler
            }

            antiSprint = true

            waitUntil { !player.isSprinting && !player.lastSprinting }

            waitTicks(reSprintTicks.random())

            antiSprint = false
        }
    }

    object WTap : Choice("WTap") {
        override val parent: ChoiceConfigurable
            get() = modes

        val ticksUntilMovementBlock by intRange("TicksUntilMovementBlock", 0..1, 0..10)
        val ticksUntilAllowedMovement by intRange("TicksUntilAllowedMovement", 0..1, 0..10)

        var stopMoving = false

        override fun enable() {
            stopMoving = false
        }

        val attackHandler = sequenceHandler<AttackEvent> { event ->
            if (!shouldStopSprinting(event)) {
                return@sequenceHandler
            }

            waitTicks(ticksUntilMovementBlock.random())

            stopMoving = true

            waitUntil { !player.input.hasForwardMovement() }

            waitTicks(ticksUntilAllowedMovement.random())

            stopMoving = false
        }
    }

    fun shouldBlockSprinting() = enabled && SprintTap.isActive && SprintTap.antiSprint

    fun shouldStopMoving() = enabled && WTap.isActive && WTap.stopMoving

    private suspend fun <T : Event> Sequence<T>.waitTicks(ticks: Int) {
        if (ticks > 0) {
            val time = System.currentTimeMillis() + ticks * 50L

            waitUntil { System.currentTimeMillis() >= time }
        }
    }

    private suspend fun <T : Event> Sequence<T>.shouldStopSprinting(event: AttackEvent): Boolean {
        val enemy = event.enemy

        if (!player.isSprinting && !player.lastSprinting) {
            return false
        }

        val doWorkaround = !player.isSprinting && player.lastSprinting

        // TODO: remove this once the issue with sequence type events being a bit late to detect player.isSprinting so these modes perform even better
        if (doWorkaround) {
            sync()
        }

        if (!player.isSprinting || !player.lastSprinting) {
            return false
        }

        return enemy is LivingEntity && enemy.hurtTime <= hurtTime && chance > Random.nextInt(
            0, 99
        ) && !ModuleCriticals.wouldCrit()
    }

    enum class SprintStopMode(override val choiceName: String) : NamedChoice {
        Packet("Packet"), SprintTap("SprintTap"), WTap("WTap")
    }

}
