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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

/**
 * An eagle module
 *
 * Legit trick to build faster.
 */
object ModuleEagle : Module("Eagle", Category.PLAYER, aliases = arrayOf("FastBridge", "BridgeAssistant")) {

    private val edgeDistance by float("EagleEdgeDistance", 0.4f, 0.01f..1.3f)

    private object Conditional : ToggleableConfigurable(this, "Conditional", true) {
        val holdingBlocks by boolean("HoldingBlocks", false)
        val onGround by boolean("OnGround", true)
        val pitch by floatRange("Pitch", -90f..90f, -90f..90f)
        val sneak by boolean("Sneak", false)
        val left by boolean("Left", false)
        val right by boolean("Right", false)
        val forwards by boolean("Forwards", false)
        val backwards by boolean("Backwards", false)

        fun shouldSneak(event: MovementInputEvent) = when {
            !enabled -> true
            holdingBlocks && !isValidBlock(player.mainHandStack) && !isValidBlock(player.offHandStack) -> false
            onGround && !player.isOnGround -> false
            player.pitch !in pitch -> false
            sneak && !event.sneaking -> false
            left && !event.directionalInput.left -> false
            right && !event.directionalInput.right -> false
            forwards && !event.directionalInput.forwards -> false
            backwards && !event.directionalInput.backwards -> false
            else -> true
        }
    }

    init {
        tree(Conditional)
    }

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent>(
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) { event ->
        val shouldBeActive = !player.abilities.flying && Conditional.shouldSneak(event)

        event.sneaking = shouldBeActive && player.isCloseToEdge(event.directionalInput, edgeDistance.toDouble())
    }

}
