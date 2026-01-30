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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.SAFETY_FEATURE
import net.ccbluex.liquidbounce.utils.kotlin.random
import java.util.function.Predicate

/**
 * An eagle module
 *
 * Legit trick to build faster.
 */
object ModuleEagle : ClientModule(
    "Eagle", ModuleCategories.PLAYER,
    aliases = listOf("FastBridge", "BridgeAssistant", "LegitScaffold")
) {

    private val edgeDistance by floatRange("EdgeDistance", 0.4f..0.6f, 0.01f..1.3f)
        .onChanged {
            currentEdgeDistance = it.random()
        }

    private var currentEdgeDistance: Float = edgeDistance.random()
    private var wasSneaking = false
    private var sneakCaptured = false

    private fun shouldActivateEagle(event: MovementInputEvent, conditionsMet: Boolean): Boolean {
        if (player.abilities.flying || !conditionsMet) {
            return false
        }

        return player.isCloseToEdge(event.directionalInput, currentEdgeDistance.toDouble())
    }

    private fun updateSneakCapture(originalSneak: Boolean, active: Boolean) {
        if (!Conditional.controlsSneak) {
            sneakCaptured = false
            return
        }

        when {
            !sneakCaptured && active && originalSneak -> sneakCaptured = true
            sneakCaptured && !originalSneak -> sneakCaptured = false
        }
    }

    private fun shouldOverrideSneak(conditionsMet: Boolean, active: Boolean): Boolean {
        return conditionsMet && Conditional.controlsSneak && (active || sneakCaptured)
    }

    private fun updateSneakState(isSneaking: Boolean) {
        if (isSneaking) {
            wasSneaking = true
            return
        }

        if (wasSneaking) {
            currentEdgeDistance = edgeDistance.random()
            wasSneaking = false
        }
    }

    private object Conditional : ToggleableValueGroup(this, "Conditional", true) {
        private val conditions by multiEnumChoice(
            "Conditions",
            Condition.ON_GROUND
        )

        val pitch by floatRange("Pitch", -90f..90f, -90f..90f)

        val controlsSneak
            get() = enabled && Condition.SNEAK in conditions

        fun shouldSneak(event: MovementInputEvent) =
            !enabled || player.xRot in pitch && conditions.all { it.test(event) }

        @Suppress("unused")
        private enum class Condition(override val tag: String) : Tagged, Predicate<MovementInputEvent> {
            LEFT("Left"),
            RIGHT("Right"),
            FORWARDS("Forwards"),
            BACKWARDS("Backwards"),
            HOLDING_BLOCKS("HoldingBlocks"),
            ON_GROUND("OnGround"),
            SNEAK("Sneak");

            override fun test(event: MovementInputEvent): Boolean = when (this) {
                LEFT -> event.directionalInput.left
                RIGHT -> event.directionalInput.right
                FORWARDS -> event.directionalInput.forwards
                BACKWARDS -> event.directionalInput.backwards
                HOLDING_BLOCKS -> isValidBlock(player.mainHandItem) || isValidBlock(player.offhandItem)
                ON_GROUND -> player.onGround()
                SNEAK -> event.sneak
            }
        }
    }

    init {
        tree(Conditional)
    }

    override fun onDisabled() {
        wasSneaking = false
        sneakCaptured = false
        super.onDisabled()
    }

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent>(priority = SAFETY_FEATURE) { event ->
        debugParameter("EdgeDistance") { currentEdgeDistance }

        val originalSneak = mc.options.keyShift.isDown
        val conditionsMet = Conditional.shouldSneak(event)
        val isActive = shouldActivateEagle(event, conditionsMet)

        updateSneakCapture(originalSneak, isActive)

        val controlsSneak = shouldOverrideSneak(conditionsMet, isActive)

        event.sneak = if (controlsSneak) {
            isActive
        } else {
            originalSneak || isActive
        }

        updateSneakState(event.sneak)
    }

}
