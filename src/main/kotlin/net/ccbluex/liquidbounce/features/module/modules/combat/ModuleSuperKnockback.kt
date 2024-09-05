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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
 * SuperKnockback module
 *
 * Increases knockback dealt to other entities.
 */
@IncludeModule
object ModuleSuperKnockback : Module("SuperKnockback", Category.COMBAT, aliases = arrayOf("WTap")) {

    val modes = choices("Mode", Packet, arrayOf(Packet, SprintTap, WTap))
    val hurtTime by int("HurtTime", 10, 0..10)
    val chance by int("Chance", 100, 0..100, "%")
    val onlyOnGround by boolean("OnlyOnGround", false)

    private object OnlyOnMove : ToggleableConfigurable(this, "OnlyOnMove", true) {
        val onlyForward by boolean("OnlyForward", true)
    }

    init {
        tree(OnlyOnMove)
    }

    var sequence: Sequence<DummyEvent>? = null

    init {
        modes.onChange {
            reset()
            it
        }
    }

    override fun handleEvents(): Boolean {
        val handleEvents = super.handleEvents()

        // Reset if the module is not handling events anymore
        if (!handleEvents) {
            reset()
        }

        return handleEvents
    }

    object Packet : Choice("Packet") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val attackHandler = handler<AttackEvent> { event ->
            if (!shouldOperate()) {
                return@handler
            }

            val enemy = event.enemy

            if (enemy is LivingEntity && enemy.hurtTime <= hurtTime && chance >= (0..100).random() &&
                !ModuleCriticals.wouldCrit()) {
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
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val reSprintTicks by intRange("ReSprint", 0..1, 0..10, "ticks")

        var antiSprint = false

        @Suppress("unused")
        val attackHandler = handler<AttackEvent> { event ->
            if (!shouldOperate() || !shouldStopSprinting(event) || sequence != null) {
                return@handler
            }

            runWithDummyEvent {
                antiSprint = true

                it.waitUntil { !player.isSprinting && !player.lastSprinting }
                it.waitTicks(reSprintTicks.random())

                antiSprint = false
            }
        }
    }

    object WTap : Choice("WTap") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val ticksUntilMovementBlock by intRange("UntilMovementBlock", 0..1, 0..10,
            "ticks")
        val ticksUntilAllowedMovement by intRange("UntilAllowedMovement", 0..1, 0..10,
            "ticks")

        var stopMoving = false

        @Suppress("unused")
        val attackHandler = handler<AttackEvent> { event ->
            if (!shouldOperate() || !shouldStopSprinting(event) || sequence != null) {
                return@handler
            }

            runWithDummyEvent {
                it.waitTicks(ticksUntilMovementBlock.random())
                stopMoving = true
                it.waitUntil { !player.input.hasForwardMovement() }
                it.waitTicks(ticksUntilAllowedMovement.random())
                stopMoving = false
            }
        }
    }

    fun shouldBlockSprinting() = enabled && SprintTap.isActive && SprintTap.antiSprint

    fun shouldStopMoving() = enabled && WTap.isActive && WTap.stopMoving

    private fun shouldStopSprinting(event: AttackEvent): Boolean {
        val enemy = event.enemy

        if (!player.isSprinting || !player.lastSprinting) {
            return false
        }

        return enemy is LivingEntity && enemy.hurtTime <= hurtTime && chance >= (0..100).random()
            && !ModuleCriticals.wouldCrit()
    }

    private fun shouldOperate(): Boolean {
        if (onlyOnGround && !player.isOnGround) {
            return false
        }

        if (OnlyOnMove.enabled) {
            val isMovingSideways = player.input.movementSideways != 0f
            val isMoving = player.input.movementForward != 0f || isMovingSideways

            if (!isMoving || (OnlyOnMove.onlyForward && isMovingSideways)) {
                return false
            }
        }

        return true
    }

    private fun reset() {
        sequence?.cancel()
        sequence = null

        WTap.stopMoving = false
        SprintTap.antiSprint = false
    }

    private fun runWithDummyEvent(action: suspend (Sequence<DummyEvent>) -> Unit) {
        sequence = Sequence(this, {
            action(this)
        }, DummyEvent())

        sequence = null
    }

}
