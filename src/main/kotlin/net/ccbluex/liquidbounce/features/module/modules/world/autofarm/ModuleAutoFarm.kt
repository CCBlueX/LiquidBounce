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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ccbluex.agent.skills.ItemMatch
import net.ccbluex.agent.skills.farm.Farm
import net.ccbluex.agent.skills.farm.farm
import net.ccbluex.agent.tick.TickScope
import net.ccbluex.agent.tick.waitTicks
import net.ccbluex.agent.v26_3.toAgent
import net.ccbluex.agent.v26_3.toMinecraft
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.navigation.Navigator
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPoint
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult

/**
 * AutoFarm module
 *
 * Harvests ripe crops and sows them again with Minecraft Agent's farm skill, clicking only blocks in sight.
 */
object ModuleAutoFarm : ClientModule("AutoFarm", ModuleCategories.WORLD) {

    private val range by float("Range", 5F, 1F..6F)

    // The ticks to wait after interacting with something
    private val interactDelay by intRange("InteractDelay", 2..3, 1..15, "ticks")

    private val fortune by boolean("UseFortune", true)
    private val disableOnFullInventory by boolean("DisableOnFullInventory", false)

    private object AutoPlaceCrops : ToggleableValueGroup(this, "AutoPlant", true, aliases = listOf("AutoPlace"))

    internal object AutoUseBoneMeal : ToggleableValueGroup(this, "AutoUseBoneMeal", false) {
        val useDelay by intRange("UseDelay", 20..200, 0..20000, "ms")
    }

    init {
        tree(AutoPlaceCrops)
        tree(AutoUseBoneMeal)
    }

    internal val rotations = tree(RotationsValueGroup(this))

    init {
        tree(AutoFarmAutoWalk)
        tree(AutoFarmBlockESP)
    }

    // The walk's settings stay under AutoWalk, where they were before the farm skill
    private val navigator = Navigator(this, AutoFarmAutoWalk, rotations)
    private val scope = TickScope()
    private var job: Job? = null
    private var hadSpace = true

    /** The block the farm looks at to click it, while it farms. */
    val currentTarget: BlockPos?
        get() {
            if (job == null) return null
            val hit = traceFromPoint(
                range = range.toDouble(),
                start = player.eyePosition,
                direction = RotationManager.serverRotation.directionVector,
                entity = player,
            )
            return hit.takeIf { it.type == HitResult.Type.BLOCK }?.blockPos
        }

    private fun farm(): Farm {
        val walks = AutoFarmAutoWalk.enabled
        val area = if (walks) {
            // Within 32 blocks across and 4 up or down of the player
            AABB.ofSize(player.position(), 64.0, 8.0, 64.0).toAgent()
        } else {
            AABB.ofSize(player.eyePosition, range * 2.0, range * 2.0, range * 2.0).toAgent()
        }
        val boneMealDelay = AutoUseBoneMeal.useDelay
        return Farm(
            area = area,
            canWalk = walks,
            canPlant = AutoPlaceCrops.enabled,
            canWalkToPlant = AutoFarmAutoWalk.toPlant,
            canUseBoneMeal = AutoUseBoneMeal.enabled,
            shouldUseFortune = fortune,
            pickUp = if (AutoFarmAutoWalk.ToItems.enabled) ItemMatch { AutoFarmAutoWalk.ToItems.shouldPickUp(it.toMinecraft()) } else null,
            pickUpRadius = AutoFarmAutoWalk.ToItems.range.toDouble(),
            delayTicks = interactDelay,
            boneMealDelayTicks = boneMealDelay.first / 50..boneMealDelay.last / 50,
        )
    }

    @Suppress("unused")
    private val inventoryHandler = tickHandler {
        val hasSpace = hasInventorySpace()
        if (disableOnFullInventory && !hasSpace) {
            notification(message("inventoryFull"), message("disabled"), NotificationEvent.Severity.ERROR)
            enabled = false
            return@tickHandler
        }
        if (!hasSpace && hadSpace && AutoFarmAutoWalk.enabled && AutoFarmAutoWalk.ToItems.enabled) {
            notification(message("inventoryFull"), message("noLongerCollecting"), NotificationEvent.Severity.ERROR)
        }
        hadSpace = hasSpace
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        // Waits while the user is in a screen like the inventory, or Blink holds the packets back
        if (job == null || mc.gui.screen() is AbstractContainerScreen<*> || ModuleBlink.running) {
            return@handler
        }
        scope.tick()
        navigator.tick(event)
    }

    override fun onEnabled() {
        ChunkScanner.subscribe(AutoFarmBlockTracker)
        hadSpace = true
        job = scope.launch {
            while (true) {
                navigator.agent.farm(farm())
                waitTicks(10)
            }
        }
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(AutoFarmBlockTracker)
        job = null
        scope.cancel()
        navigator.reset()
    }

}
