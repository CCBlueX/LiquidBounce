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

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.ccbluex.liquidbounce.utils.entity.usingItemOrNull
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.HeldItemTrajectoryResolver
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.TridentItem

/**
 * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
 */
object AutoBowAimbotFeature : ToggleableValueGroup(ModuleAutoBow, "BowAimbot", true) {

    val targetTracker = TargetTracker(TargetPriority.DISTANCE)
    private val rotations = RotationsValueGroup(this)
    private val throughWalls by boolean("ThroughWalls", true)

    init {
        tree(targetTracker)
        tree(rotations)
        tree(TargetRenderer(AutoBowAimbotFeature, targetTracker))
    }

    /**
     * Where to aim at [TargetTracker.target], planned once per tick for the next rotation update.
     */
    private var aim: Rotation? = null

    @Suppress("unused")
    private val tickRepeatable = handler<GameTickEvent> {
        targetTracker.reset()
        aim = null

        // Should check if player is using bow
        val activeStack = player.usingItemOrNull ?: player.handItems.firstOrNull {
            it.item is CrossbowItem && CrossbowItem.isCharged(it)
        }
        val activeItem = activeStack?.item

        if (activeItem !is BowItem && activeItem !is TridentItem && activeItem !is CrossbowItem) {
            return@handler
        }

        val trajectoryDescriptor = HeldItemTrajectoryResolver.resolveHeldItemPrimaryShot(
            player,
            activeStack,
            true
        ) ?: return@handler

        var rotation: Rotation? = null
        val calculator = if (throughWalls) {
            SituationalProjectileAngleCalculator
        } else {
            SituationalProjectileAngleCalculator.VerifyHitResult
        }
        targetTracker.selectFirst { enemy ->
            rotation = calculator.calculateAngleForEntity(trajectoryDescriptor.trajectoryInfo, enemy)
            rotation != null
        } ?: return@handler

        aim = rotation
    }

    @Suppress("unused")
    private val aimHandler = tickHandler {
        if (aim == null) {
            return@tickHandler
        }

        val planned by requires { aim }
        val onTarget = ModuleAutoBow.lookAt(Priority.IMPORTANT_FOR_USAGE_1, {
            rotations.toRotationTarget(planned, considerInventory = true)
        }) { going -> AutoBowAutoShootFeature.isAimedAt(going, planned) }

        AutoBowAutoShootFeature.shootWithAim(onTarget)
    }

}
