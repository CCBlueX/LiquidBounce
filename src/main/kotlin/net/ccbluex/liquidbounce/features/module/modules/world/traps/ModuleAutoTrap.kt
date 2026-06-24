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
package net.ccbluex.liquidbounce.features.module.modules.world.traps

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.world.traps.traps.IgnitionTrapPlanner
import net.ccbluex.liquidbounce.features.module.modules.world.traps.traps.TrapPlayerSimulation
import net.ccbluex.liquidbounce.features.module.modules.world.traps.traps.WebTrapPlanner
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

/**
 * Ignite & AutoWeb module
 *
 * Ignite: Automatically sets targets around you on fire.
 * AutoWeb: Automatically places cobwebs at targets around you.
 */
object ModuleAutoTrap : ClientModule("AutoTrap", ModuleCategories.WORLD, aliases = listOf("Ignite", "AutoWeb")) {

    private val range = floatRange("Range", 3.0f..4.5f, 2f..6f)
    private val delay by int("Delay", 20, 0..400, "ticks")
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private val ignitionTrapPlanner = tree(IgnitionTrapPlanner(this))
    private val webTrapPlanner = tree(WebTrapPlanner(this))
    val targetTracker = tree(TargetTracker(range = range))
    private val rotations = tree(RotationsValueGroup(this))

    private var currentPlan: BlockChangeIntent<*>? = null

    private var timeout = false

    override fun onEnabled() {
        resetState()
    }

    override fun onDisabled() {
        resetState()
        SilentHotbar.resetSlot(this)
    }

    private fun resetState() {
        timeout = false
        currentPlan = null
        targetTracker.reset()
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (timeout) {
            return@handler
        }

        if (!ignoreOpenInventory && mc.gui.screen() is AbstractContainerScreen<*>) {
            return@handler
        }

        val enemies = targetTracker.targets()
        TrapPlayerSimulation.runSimulations(enemies)

        currentPlan = webTrapPlanner.plan(enemies) ?: ignitionTrapPlanner.plan(enemies)
        currentPlan?.let { intent ->
            val blockChangeInfo = intent.blockChangeInfo
            if (blockChangeInfo !is BlockChangeInfo.PlaceBlock) {
                currentPlan = null
                return@handler
            }

            RotationManager.setRotationTarget(
                blockChangeInfo.blockPlacementTarget.rotation,
                considerInventory = !ignoreOpenInventory,
                valueGroup = rotations,
                Priority.IMPORTANT_FOR_PLAYER_LIFE,
                this
            )
        }
    }

    @Suppress("unused")
    private val placementHandler = tickHandler {
        if (!ignoreOpenInventory && mc.gui.screen() is AbstractContainerScreen<*>) {
            return@tickHandler
        }

        val plan = currentPlan ?: return@tickHandler
        if (plan.blockChangeInfo !is BlockChangeInfo.PlaceBlock) {
            currentPlan = null
            return@tickHandler
        }

        if (shouldWaitForTiming(plan)) {
            return@tickHandler
        }

        val raycast = traceFromPlayer()
        if (!plan.validate(raycast)) {
            return@tickHandler
        }

        CombatManager.pauseCombatForAtLeast(1)
        SilentHotbar.selectSlotSilently(this, plan.slot, 1)

        var successful = false
        val onSuccess = {
            plan.onIntentFulfilled()
            successful = true
            true
        }

        doPlacement(
            raycast,
            hand = plan.slot.useHand,
            onPlacementSuccess = onSuccess,
            onItemUseSuccess = onSuccess
        )

        if (!successful) {
            return@tickHandler
        }

        timeout = true
        try {
            waitTicks(delay)
        } finally {
            timeout = false
        }
    }

    private fun shouldWaitForTiming(plan: BlockChangeIntent<*>): Boolean {
        return when (plan.timing) {
            IntentTiming.INSTANT -> false

            // Let ongoing combat modules consume the current hit window first, then place during recovery.
            IntentTiming.NEXT_PROPITIOUS_MOMENT -> hasPendingCombatAction() && (
                player.getAttackStrengthScale(0.5f) > 0.9f
                    || ModuleCriticals.wouldDoCriticalHit(ignoreSprint = true)
                )
        }
    }

    private fun hasPendingCombatAction(): Boolean {
        return ModuleKillAura.running && ModuleKillAura.targetTracker.target != null
    }
}
