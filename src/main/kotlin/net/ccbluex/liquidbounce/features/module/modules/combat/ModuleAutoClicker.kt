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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.CriticalsSelectionMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.item.AxeItem
import net.minecraft.item.BlockItem
import net.minecraft.item.SwordItem
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult

/**
 * AutoClicker module
 *
 * Clicks automatically when holding down a mouse button.
 */

object ModuleAutoClicker : ClientModule("AutoClicker", Category.COMBAT, aliases = arrayOf("TriggerBot")) {

    object AttackButton : ToggleableConfigurable(this, "Attack", true) {

        val clicker = tree(Clicker(this, mc.options.attackKey, true))
        internal val requiresNoInput by boolean("RequiresNoInput", false)
        private val objectiveType by enumChoice("Objective", ObjectiveType.ANY)
        private val onItemUse by enumChoice("OnItemUse", Use.WAIT)
        private val weapon by enumChoice("Weapon", Weapon.ANY)
        private val criticalsSelectionMode by enumChoice("Criticals", CriticalsSelectionMode.SMART)
        private val delayPostStopUse by int("DelayPostStopUse", 0, 0..20, "ticks")

        enum class ObjectiveType(override val choiceName: String) : NamedChoice {
            ENEMY("Enemy"),
            ENTITY("Entity"),
            BLOCK("Block"),
            ANY("Any")
        }

        enum class Weapon(override val choiceName: String) : NamedChoice {
            SWORD("Sword"),
            AXE("Axe"),
            BOTH("Both"),
            ANY("Any")
        }

        enum class Use(override val choiceName: String) : NamedChoice {
            WAIT("Wait"),
            STOP("Stop"),
            IGNORE("Ignore")
        }

        fun isOnObjective(): Boolean {
            val crosshair = mc.crosshairTarget

            return when (objectiveType) {
                ObjectiveType.ENEMY -> crosshair is EntityHitResult && crosshair.entity.shouldBeAttacked()
                ObjectiveType.ENTITY -> crosshair is EntityHitResult
                ObjectiveType.BLOCK -> crosshair is BlockHitResult
                ObjectiveType.ANY -> true
            }
        }

        fun isWeaponSelected(): Boolean {
            val item = player.mainHandStack.item

            return when (weapon) {
                Weapon.SWORD -> item is SwordItem
                Weapon.AXE -> item is AxeItem
                Weapon.BOTH -> item is SwordItem || item is AxeItem
                Weapon.ANY -> true
            }
        }

        fun isCriticalHit(entity: Entity): Boolean {
            return criticalsSelectionMode.isCriticalHit(entity)
        }

        suspend fun Sequence.encounterItemUse(): Boolean {
            return when (onItemUse) {
                Use.WAIT -> {
                    this.waitUntil { !player.isUsingItem }

                    if (delayPostStopUse > 0) {
                        waitTicks(delayPostStopUse)
                    }

                    true
                }

                Use.STOP -> {
                    interaction.stopUsingItem(player)

                    if (delayPostStopUse > 0) {
                        waitTicks(delayPostStopUse)
                    }

                    true
                }

                Use.IGNORE -> false
            }
        }

        @Suppress("unused")
        private val sprintHandler = handler<SprintEvent> { event ->
            if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
                if (!attack || !isOnObjective() || !isWeaponSelected()) {
                    return@handler
                }

                val target = mc.crosshairTarget as? EntityHitResult ?: return@handler
                if (criticalsSelectionMode.shouldStopSprinting(clicker, target.entity)) {
                    event.sprint = false
                }
            }
        }

    }

    object UseButton : ToggleableConfigurable(this, "Use", false) {
        val clicker = tree(Clicker(this, mc.options.useKey, false))
        internal val delayStart by boolean("DelayStart", false)
        internal val onlyBlock by boolean("OnlyBlock", false)
        internal val requiresNoInput by boolean("RequiresNoInput", false)

        internal var needToWait = true
    }

    init {
        tree(AttackButton)
        tree(UseButton)
    }

    val attack: Boolean
        get() = mc.options.attackKey.isPressedOnAny || AttackButton.requiresNoInput

    val use: Boolean
        get() = mc.options.useKey.isPressedOnAny || UseButton.requiresNoInput

    @Suppress("unused")
    val tickHandler = tickHandler {
        AttackButton.run {
            if (!enabled || !attack || !isWeaponSelected() || !isOnObjective()) {
                return@run
            }

            // Check if the player is breaking a block, if so, return
            if (interaction.isBreakingBlock) {
                return@run
            }

            val crosshairTarget = mc.crosshairTarget
            if (crosshairTarget is EntityHitResult) {
                ModuleAutoWeapon.prepare(crosshairTarget.entity)

                if (!isCriticalHit(crosshairTarget.entity)) {
                    return@run
                }
            }

            if (player.usingItem) {
                val encounterItemUse = encounterItemUse()

                if (encounterItemUse) {
                    return@tickHandler
                }
            }

            clicker.click {
                KeyBinding.onKeyPressed(mc.options.attackKey.boundKey)
                true
            }
        }

        UseButton.run {
            if (!enabled) return@run

            if (!use) {
                needToWait = true
                return@run
            }

            if (onlyBlock && player.mainHandStack.item !is BlockItem) {
                return@run
            }

            if (delayStart && needToWait) {
                needToWait = false
                waitTicks(2)
                return@run
            }

            clicker.click {
                KeyBinding.onKeyPressed(mc.options.useKey.boundKey)
                true
            }
        }
    }
}
