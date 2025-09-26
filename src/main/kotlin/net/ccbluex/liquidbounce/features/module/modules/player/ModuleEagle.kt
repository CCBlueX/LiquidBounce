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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
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
object ModuleEagle : ClientModule("Eagle", Category.PLAYER,
    aliases = listOf("FastBridge", "BridgeAssistant", "LegitScaffold")
) {

    private val edgeDistance by floatRange("EdgeDistance", 0.4f..0.6f, 0.01f..1.3f)
        .onChanged {
            currentEdgeDistance = it.random()
        }

    private var currentEdgeDistance: Float = edgeDistance.random()
    private var wasSneaking = false

    private object Conditional : ToggleableConfigurable(this, "Conditional", true) {
        private val conditions by multiEnumChoice("Conditions",
            Condition.ON_GROUND
        )

        val pitch by floatRange("Pitch", -90f..90f, -90f..90f)

        fun shouldSneak(event: MovementInputEvent) =
            !enabled || player.pitch in pitch && conditions.all { it.test(event) }

        @Suppress("unused")
        private enum class Condition(override val choiceName: String) : NamedChoice, Predicate<MovementInputEvent> {
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
                HOLDING_BLOCKS -> isValidBlock(player.mainHandStack) || isValidBlock(player.offHandStack)
                ON_GROUND -> player.isOnGround
                SNEAK -> event.sneak
            }
        }
    }

    init {
        tree(Conditional)
    }

    override fun onDisabled() {
        wasSneaking = false
        super.onDisabled()
    }

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent>(priority = SAFETY_FEATURE) { event ->
        debugParameter("EdgeDistance") { currentEdgeDistance }

        val shouldBeActive = !player.abilities.flying && Conditional.shouldSneak(event) &&
            player.isCloseToEdge(event.directionalInput, currentEdgeDistance.toDouble())

        event.sneak = event.sneak && !Conditional.shouldSneak(event) || shouldBeActive

        if (event.sneak) {
            wasSneaking = true
        } else if (wasSneaking) {
            currentEdgeDistance = edgeDistance.random()
            wasSneaking = false
        }
    }

}
