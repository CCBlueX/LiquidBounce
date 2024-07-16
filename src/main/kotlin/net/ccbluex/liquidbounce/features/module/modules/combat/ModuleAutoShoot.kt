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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.util.Hand
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

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

    private val throwableType by enumChoice("ThrowableType", ThrowableType.EGG_AND_SNOWBALL)
    private val gravityType by enumChoice("GravityType", GravityType.AUTO)

    private val clickScheduler = tree(ClickScheduler(this, showCooldown = false))

    /**
     * The target tracker to find the best enemy to attack.
     */
    internal val targetTracker = tree(TargetTracker(defaultPriority = PriorityEnum.DISTANCE))
    private val pointTracker = tree(
        PointTracker(
            lowestPointDefault = PointTracker.PreferredBoxPart.HEAD,
            highestPointDefault = PointTracker.PreferredBoxPart.HEAD,
            // The lag on Hypixel is massive
            timeEnemyOffsetDefault = 3f,
            timeEnemyOffsetScale = 0f..7f,
            gaussianOffsetDefault = false
        )
    )

    /**
     * So far I have never seen an anti-cheat which detects high turning speed for actions such as
     * shooting.
     */
    private val rotationConfigurable = tree(RotationsConfigurable(this))
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f)

    /**
     * The target renderer to render the target, which we are currently aiming at.
     */
    private val targetRenderer = tree(WorldTargetRenderer(this))

    private val selectSlotAutomatically by boolean("SelectSlotAutomatically", true)
    private val considerInventory by boolean("ConsiderInventory", true)

    private val notDuringCombat by boolean("NotDuringCombat", false)
    val constantLag by boolean("ConstantLag", false)

    /**
     * Simulates the next tick, which we use to figure out the required rotation for the next tick to react
     * as fast possible. This means we already pre-aim before we peek around the corner.
     */
    @Suppress("unused")
    val simulatedTickHandler = handler<SimulatedTickEvent> {
        targetTracker.cleanup()

        if (notDuringCombat && CombatManager.isInCombat()) {
            return@handler
        }

        // Check if we have a throwable, if not we can't shoot.
        val (hand, slot) = getThrowable() ?: return@handler

        // Find the recommended target
        val target = targetTracker.enemies().firstOrNull {
            // Check if we can see the enemy
            player.canSee(it)
        } ?: return@handler

        // Select the throwable if we are not holding it.
        if (slot != -1) {
            if (!selectSlotAutomatically) {
                return@handler
            }
            SilentHotbar.selectSlotSilently(this, slot)
        }

        val rotation = generateRotation(target, GravityType.fromHand(hand))

        // Set the rotation with the usage priority of 2.
        RotationManager.aimAt(
            rotationConfigurable.toAimPlan(rotation ?: return@handler, considerInventory = considerInventory),
            Priority.IMPORTANT_FOR_USAGE_2, this
        )
        targetTracker.lock(target)
    }

    /**
     * Handles the auto shoot logic.
     */
    @Suppress("unused")
    val handleAutoShoot = repeatable {
        val target = targetTracker.lockedOnTarget ?: return@repeatable

        // Cannot happen but we want to smart-cast
        if (target !is LivingEntity) {
            return@repeatable
        }

        if (notDuringCombat && CombatManager.isInCombat()) {
            return@repeatable
        }

        // Check if we have a throwable, if not we can't shoot.
        val (hand, slot) = getThrowable() ?: return@repeatable

        // Select the throwable if we are not holding it.
        if (slot != -1) {
            SilentHotbar.selectSlotSilently(this, slot)

            // If we are not holding the throwable, we can't shoot.
            if (SilentHotbar.serversideSlot != slot) {
                return@repeatable
            }
        }

        // Select the throwable if we are not holding it.
        if (slot != -1) {
            SilentHotbar.selectSlotSilently(this, slot)
        }

        val rotation = generateRotation(target, GravityType.fromHand(hand))

        // Check difference between server and client rotation
        val rotationDifference = RotationManager.rotationDifference(
            rotation ?: return@repeatable,
            RotationManager.serverRotation
        )

        // Check if we are not aiming at the target yet
        if (rotationDifference > aimOffThreshold) {
            return@repeatable
        }

        // Check if we are still aiming at the target
        clickScheduler.clicks {
            if (player.isUsingItem || (considerInventory && InventoryManager.isInventoryOpenServerSide)) {
                return@clicks false
            }

            interaction.interactItem(player, hand).isAccepted
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val target = targetTracker.lockedOnTarget ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }

    private fun generateRotation(target: LivingEntity, gravityType: GravityType): Rotation? {
        val (fromPoint, toPoint, _, _)
                = pointTracker.gatherPoint(target, PointTracker.AimSituation.FOR_NEXT_TICK)

        return when (gravityType) {

            GravityType.AUTO -> {
                // Should not happen, we convert [gravityType] to LINEAR or PROJECTILE before.
                return null
            }

            GravityType.LINEAR -> {
                RotationManager.makeRotation(toPoint, fromPoint)
            }

            // Determines the required yaw and pitch angles to hit a target with a projectile,
            // considering gravity's effect on the projectile's motion.
            GravityType.PROJECTILE -> {
                // The velocity of the projectile at the moment of launch, determined by testing.
                // todo: use math: eggEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
                val targetPosition = toPoint.subtract(fromPoint)

                val launchVelocity = 0.6f

                // Compute the horizontal distance to the target on the XZ plane (ignoring y-component).
                val horizontalDistance = sqrt(
                    targetPosition.x * targetPosition.x +
                            targetPosition.z * targetPosition.z
                )

                // Calculate yaw angle: the horizontal angle between the player's forward direction
                // and the direction to the target, in radians converted to degrees and adjusted by -90°.
                val yaw = (atan2(targetPosition.z, targetPosition.x) * 180.0f / Math.PI).toFloat() - 90.0f

                // Calculate the pitch angle required to hit the target by solving the projectile
                // motion equation, considering gravity and initial launch velocity.
                val pitch = (-Math.toDegrees(
                    atan(
                        (launchVelocity.pow(2) - sqrt(
                            launchVelocity.pow(4) -
                                    0.006f * (0.006f * (horizontalDistance.pow(2)) + 2 * targetPosition.y *
                                    launchVelocity.pow(2))
                        )) / (0.006f * horizontalDistance)
                    )
                )).toFloat()

                // Check if the calculated yaw and pitch are valid numbers; if not, return null.
                if (yaw.isNaN() || pitch.isNaN()) {
                    return null
                }

                // Return the computed Rotation object containing the yaw and pitch angles.
                Rotation(yaw, pitch)
            }
        }
    }

    private fun getThrowable(): Pair<Hand, Int>? {
        return when (throwableType) {
            ThrowableType.EGG_AND_SNOWBALL -> getThrowable(Items.EGG) ?: getThrowable(Items.SNOWBALL)
            ThrowableType.ANYTHING -> {
                if (player.mainHandStack?.isNothing() == false) {
                    Hand.MAIN_HAND to -1
                } else if (player.offHandStack?.isNothing() == false) {
                    Hand.OFF_HAND to -1
                } else {
                    null
                }
            }
        }
    }

    private fun getThrowable(item: Item): Pair<Hand, Int>? {
        val mainHand = player.mainHandStack.item == item
        val offHand = player.offHandStack.item == item

        // If both is false, we have to find the item in the hotbar
        return if (!mainHand && !offHand) {
            val throwableSlot = findHotbarSlot(item) ?: return null
            Hand.MAIN_HAND to throwableSlot
        } else if (offHand) {
            Hand.OFF_HAND to -1
        } else { // mainHand
            Hand.MAIN_HAND to -1
        }
    }

    private enum class ThrowableType(override val choiceName: String) : NamedChoice {
        EGG_AND_SNOWBALL("Egg and Snowball"),
        ANYTHING("Anything"),
    }

    private enum class GravityType(override val choiceName: String) : NamedChoice {

        AUTO("Auto"),
        LINEAR("Linear"),
        PROJECTILE("Projectile");

        companion object {
            fun fromHand(hand: Hand): GravityType {
                return when (hand) {
                    Hand.MAIN_HAND -> fromItem(player.mainHandStack.item)
                    Hand.OFF_HAND -> fromItem(player.offHandStack.item)
                }
            }

            fun fromItem(item: Item): GravityType {
                return when (gravityType) {
                    AUTO -> {
                        when (item) {
                            Items.EGG, Items.SNOWBALL -> PROJECTILE
                            else -> LINEAR
                        }
                    }

                    else -> gravityType
                }
            }
        }

    }

}
