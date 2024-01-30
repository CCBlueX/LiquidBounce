/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.Entity
import net.minecraft.util.Hand

/**
 * A module that automatically shoots at the nearest enemy.
 *
 * Specifically designed for Hypixel QuakeCraft.
 * However, I mostly have tested them for other game modes such as Cytooxien Lasertag and Paintball.
 *
 * It also replaces our AutoBalls module as it is more accurate.
 *
 * @author 1zuna
 */
object ModuleAutoShoot : Module("AutoShoot", Category.COMBAT) {

    val clickScheduler = tree(ClickScheduler(this, showCooldown = false))

    /**
     * The target tracker to find the best enemy to attack.
     */
    val targetTracker = tree(TargetTracker(defaultPriority = PriorityEnum.DISTANCE))

    /**
     * So far I have never seen an anti-cheat which detects high turning speed for actions such as
     * shooting.
     */
    val rotationConfigurable = tree(RotationsConfigurable(turnSpeed = 180f..180f))

    val considerInventory by boolean("ConsiderInventory", true)

    /**
     * Simulates the next tick, which we use to figure out the required rotation for the next tick to react
     * as fast possible. This means we already pre-aim before we peek around the corner.
     */
    val simulatedTickHandler = handler<SimulatedTickEvent> {
        targetTracker.cleanup()

        // Find the recommended target
        val target = targetTracker.enemies().firstOrNull {
            // Check if we can see the enemy
            player.canSee(it)
        } ?: return@handler

        val rotation = generateRotation(target, GravityType.LINEAR)

        // Set the rotation with the usage priority of 2.
        RotationManager.aimAt(rotationConfigurable.toAimPlan(rotation, considerInventory),
            Priority.IMPORTANT_FOR_USAGE_2, this)
    }

    /**
     *
     */
    val handleAutoShoot = repeatable {
        val target = targetTracker.lockedOnTarget ?: return@repeatable

        // TODO: Add checking for eggs and snowballs depending on the option of the user

        // Check if we are still aiming at the target
        clickScheduler.clicks {
            if (player.isUsingItem || (considerInventory && InventoryTracker.isInventoryOpenServerSide)) {
                return@clicks false
            }

            interaction.interactItem(player, Hand.MAIN_HAND).isAccepted
        }
    }

    private fun generateRotation(target: Entity, gravityType: GravityType): Rotation {
        val eyesOfPlayer = player.eyes

        // TODO: Implement gravity types, such as egg and snowball. Currently we expect linear movement, which
        //  is not the case for eggs and snowballs.
        return when (gravityType) {
            GravityType.LINEAR -> {
                val headOfEnemy = target.eyes

                RotationManager.makeRotation(headOfEnemy, eyesOfPlayer)
            }
            GravityType.EGG -> TODO()
            GravityType.SNOWBALL -> TODO()
        }
    }

    private enum class GravityType {
        LINEAR,

        // TODO: Check if egg and snowball have the same gravity or not.
        EGG,
        SNOWBALL,
    }

}
