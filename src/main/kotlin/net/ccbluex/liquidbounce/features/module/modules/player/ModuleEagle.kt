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

/**
 * An eagle module
 *
 * Legit trick to build faster.
 */
object ModuleEagle : ClientModule(
    "Eagle", Category.PLAYER,
    aliases = arrayOf("FastBridge", "BridgeAssistant", "LegitScaffold")
) {

    private val edgeDistance by floatRange("EdgeDistance", 0.4f..0.6f, 0.01f..1.3f)
        .onChanged {
            currentEdgeDistance = it.random()
        }

    private var currentEdgeDistance: Float = edgeDistance.random()
    var wasSneaking = false
    private object EagleAutoBlockFeature : ToggleableConfigurable(this, "AutoBlock", true) {
        val slotResetDelay by int("SlotResetDelay", 5, 0..40, "ticks")
        val doNotUseBelowCount by int("DoNotUseBelowCount", 1, 0..64)

        private var lastSwitch = 0

        fun trySwitchBlock(): Boolean {
            if (!enabled || player.isCreative) return false

            val handStack = player.mainHandStack
            if (isValidBlock(handStack) && handStack.count > doNotUseBelowCount) return false

            if (lastSwitch > 0) {
                lastSwitch--
                return false
            }

            for (i in 0..8) {
                val stack = player.inventory.getStack(i)
                if (isValidBlock(stack) && stack.count > doNotUseBelowCount) {
                    player.inventory.selectedSlot = i
                    lastSwitch = slotResetDelay
                    return true
                }
            }
            return false
        }
    }
    private object Conditional : ToggleableConfigurable(this, "Conditional", true) {
        private val conditions by multiEnumChoice(
            "Conditions",
            Conditions.ON_GROUND
        )

        val pitch by floatRange("Pitch", -90f..90f, -90f..90f)

        fun shouldSneak(event: MovementInputEvent) =
            !enabled || player.pitch in pitch && conditions.all { it.meetsCondition(event) }

        @Suppress("unused")
        private enum class Conditions(
            override val choiceName: String,
            val meetsCondition: (event: MovementInputEvent) -> Boolean
        ) : NamedChoice {
            LEFT("Left", { event ->
                event.directionalInput.left
            }),
            RIGHT("Right", { event ->
                event.directionalInput.right
            }),
            FORWARDS("Forwards", { event ->
                event.directionalInput.forwards
            }),
            BACKWARDS("Backwards", { event ->
                event.directionalInput.backwards
            }),
            HOLDING_BLOCKS("HoldingBlocks", { _ ->
                isValidBlock(player.mainHandStack) || isValidBlock(player.offHandStack)
            }),
            ON_GROUND("OnGround", { _ ->
                player.isOnGround
            }),
            SNEAK("Sneak", { event ->
                event.sneak
            })
        }
    }

    init {
        treeAll(Conditional, EagleAutoBlockFeature)
    }
    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent>(priority = SAFETY_FEATURE) { event ->
        debugParameter("EdgeDistance") { currentEdgeDistance }

        EagleAutoBlockFeature.trySwitchBlock()

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



    override fun onDisabled() {
        wasSneaking = false
        super.onDisabled()
    }


}
