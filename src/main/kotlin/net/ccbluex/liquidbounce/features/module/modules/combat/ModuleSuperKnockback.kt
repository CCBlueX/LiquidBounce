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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.client.sendStartSprinting
import net.ccbluex.liquidbounce.utils.client.sendStopSprinting
import net.ccbluex.liquidbounce.utils.entity.isInsideWaterOrBubbleColumn
import net.ccbluex.liquidbounce.utils.entity.movementForward
import net.ccbluex.liquidbounce.utils.entity.movementSideways
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

/**
 * SuperKnockback module
 *
 * Increases knockback dealt to other entities.
 */
@Suppress("MagicNumber")
object ModuleSuperKnockback : ClientModule("SuperKnockback", ModuleCategories.COMBAT, aliases = listOf("WTap")) {

    val modes = choices("Mode", Packet, arrayOf(Packet, SprintTap, WTap)).apply(::tagBy)
    val hurtTime by int("HurtTime", 10, 0..10)
    val chance by int("Chance", 100, 0..100, "%")
    private val conditions by multiEnumChoice("Conditions", Conditions.NOT_IN_WATER)

    @Suppress("unused")
    private enum class Conditions(
        override val tag: String,
        val testCondition: (target: Entity) -> Boolean
    ) : Tagged {
        ONLY_FACING("OnlyFacing", { target ->
            target.lookAngle.dot(player.position() - target.position()) < 0
        }),
        ONLY_ON_GROUND("OnlyOnGround", { _ ->
            player.onGround()
        }),
        NOT_IN_WATER("NotInWater", { _ ->
            !player.isInsideWaterOrBubbleColumn
        }),
    }

    private object OnlyOnMove : ToggleableValueGroup(this, "OnlyOnMove", true) {
        val onlyForward by boolean("OnlyForward", true)
    }

    init {
        tree(OnlyOnMove)
    }

    object Packet : Mode("Packet") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused", "ComplexCondition")
        private val attackHandler = handler<AttackEntityEvent> { event ->
            val enemy = event.entity

            if (!shouldOperate(enemy)) {
                return@handler
            }

            if (enemy is LivingEntity
                && enemy.hurtTime <= hurtTime && chance >= (0..100).random()
                && !ModuleCriticals.wouldDoCriticalHit()
            ) {
                if (player.isSprinting) {
                    network.sendStopSprinting()
                }

                network.sendStartSprinting()
                network.sendStopSprinting()
                network.sendStartSprinting()

                player.isSprinting = true
                player.wasSprinting = true
            }
        }
    }

    object SprintTap : Mode("SprintTap") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val reSprintTicks by intRange("ReSprint", 0..1, 0..10, "ticks")

        private var cancelSprint = false

        @Suppress("unused", "ComplexCondition")
        private val attackHandler = sequenceHandler<AttackEntityEvent>(
            onCancellation = {
                cancelSprint = false
                this@SprintTap.debugParameter("State") { "Allowing Sprint (Cancellation)" }
            }
        ) { event ->
            if (!shouldOperate(event.entity) || !shouldStopSprinting(event) || cancelSprint) {
                return@sequenceHandler
            }

            this@SprintTap.debugParameter("State") { "Disallowing Sprint" }
            cancelSprint = true
            tickUntil { !player.isSprinting && !player.wasSprinting }
            this@SprintTap.debugParameter("State") { "Waiting for ReSprint" }
            waitTicks(reSprintTicks.random())
            this@SprintTap.debugParameter("State") { "Allowing Sprint" }
            cancelSprint = false
        }

        @Suppress("unused")
        private val movementHandler = handler<SprintEvent>(
            priority = CRITICAL_MODIFICATION
        ) { event ->
            if (cancelSprint && (event.source == SprintEvent.Source.MOVEMENT_TICK ||
                    event.source == SprintEvent.Source.INPUT)) {
                event.sprint = false
            }
        }

        override fun disable() {
            cancelSprint = false
            super.disable()
        }

    }

    object WTap : Mode("WTap") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val ticksUntilMovementBlock by intRange("UntilMovementBlock", 0..1, 0..10,
            "ticks")
        private val ticksUntilAllowedMovement by intRange("UntilAllowedMovement", 0..1, 0..10,
            "ticks")

        private var inSequence = false
        private var cancelMovement = false

        @Suppress("unused", "ComplexCondition")
        private val attackHandler = sequenceHandler<AttackEntityEvent>(
            onCancellation = {
                cancelMovement = false
                inSequence = false
                this@WTap.debugParameter("State") { "Allowing Movement (Cancellation)" }
            }
        ) { event ->
            if (!shouldOperate(event.entity) || !shouldStopSprinting(event) || inSequence) {
                return@sequenceHandler
            }

            inSequence = true
            this@WTap.debugParameter("State") { "Waiting for Movement Block" }
            waitTicks(ticksUntilMovementBlock.random())
            this@WTap.debugParameter("State") { "Disallowing Movement" }
            cancelMovement = true
            tickUntil { !player.input.hasForwardImpulse() }
            this@WTap.debugParameter("State") { "Waiting for Allowed Movement" }
            waitTicks(ticksUntilAllowedMovement.random())
            this@WTap.debugParameter("State") { "Allowing Movement" }
            cancelMovement = false
            inSequence = false
        }

        @Suppress("unused")
        private val movementHandler = handler<MovementInputEvent> { event ->
            if (inSequence && cancelMovement) {
                event.directionalInput = DirectionalInput.NONE
            }
        }

        override fun disable() {
            cancelMovement = false
            inSequence = false
            super.disable()
        }

    }

    private fun shouldStopSprinting(event: AttackEntityEvent): Boolean {
        val enemy = event.entity

        if (!player.isSprinting || !player.wasSprinting) {
            return false
        }

        return enemy is LivingEntity && enemy.hurtTime <= hurtTime && chance >= (0..100).random()
            && !ModuleCriticals.wouldDoCriticalHit()
    }

    @Suppress("ReturnCount")
    private fun shouldOperate(target: Entity): Boolean {
        if (OnlyOnMove.enabled) {
            val isMovingSideways = player.input.movementSideways != 0f
            val isMoving = player.input.movementForward != 0f || isMovingSideways

            if (!isMoving || (OnlyOnMove.onlyForward && isMovingSideways)) {
                return false
            }
        }

        return conditions.all { it.testCondition(target) }
    }

}
