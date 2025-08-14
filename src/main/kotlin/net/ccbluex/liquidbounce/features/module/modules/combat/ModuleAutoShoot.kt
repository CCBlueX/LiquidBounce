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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.point.preference.PreferredBoxPart
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.interactItem
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.Items

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
object ModuleAutoShoot : ClientModule("AutoShoot", Category.COMBAT) {

    private val throwableType by enumChoice("ThrowableType", ThrowableType.EGG_AND_SNOWBALL)
    private val gravityType by enumChoice("GravityType", GravityType.AUTO).apply { tagBy(this) }

    private val clicker = tree(Clicker(this, mc.options.useKey, showCooldown = false))

    /**
     * The target tracker to find the best enemy to attack.
     */
    internal val targetTracker = tree(TargetTracker(TargetPriority.DISTANCE, floatRange("Range", 3.0f..6f, 1f..50f)))
    private val pointTracker = tree(
        PointTracker(
            lowestPointDefault = PreferredBoxPart.HEAD,
            highestPointDefault = PreferredBoxPart.HEAD,
            // The lag on Hypixel is massive
            timeEnemyOffsetDefault = 3f,
            timeEnemyOffsetScale = 0f..7f
        )
    )

    /**
     * So far, I have never seen an anti-cheat which detects high turning speed for actions such as
     * shooting.
     */
    private val rotationConfigurable = tree(RotationsConfigurable(this))
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f)

    /**
     * The target renderer to render the target, which we are currently aiming at.
     */
    private val targetRenderer = tree(WorldTargetRenderer(this))

    private val selectSlotAutomatically by boolean("SelectSlotAutomatically", true)
    private val tickUntilSlotReset by int("TicksUntillSlotReset", 1, 0..20)
    private val considerInventory by boolean("ConsiderInventory", true)

    private val requiresKillAura by boolean("RequiresKillAura", false)
    private val notDuringCombat by boolean("NotDuringCombat", false)
    val constantLag by boolean("ConstantLag", false)

    private fun HotbarItemSlot.needsSelection(): Boolean =
        this !is OffHandSlot && this.hotbarSlot != SilentHotbar.serversideSlot

    /**
     * @return If the player successfully selected [slot]
     */
    private fun trySelect(slot: HotbarItemSlot): Boolean {
        // Select the throwable if we are not holding it.
        if (slot.needsSelection()) {
            if (!selectSlotAutomatically) {
                return false
            }
            // If we are not holding the throwable, we can't shoot.
            SilentHotbar.selectSlotSilently(this, slot, tickUntilSlotReset)
            if (slot !is OffHandSlot && SilentHotbar.serversideSlot != slot.hotbarSlotForServer) {
                return false
            }
        }
        return true
    }

    /**
     * Simulates the next tick, which we use to figure out the required rotation for the next tick to react
     * as fast possible. This means we already pre-aim before we peek around the corner.
     */
    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        // Find the recommended target
        val target = targetTracker.selectFirst {
            // Check if we can see the enemy
            player.canSee(it)
        } ?: return@handler

        if (notDuringCombat && CombatManager.isInCombat) {
            return@handler
        }

        if (requiresKillAura && !ModuleKillAura.running) {
            return@handler
        }

        // Check if we have a throwable, if not we can't shoot.
        val slot = getThrowable() ?: return@handler

        if (!trySelect(slot)) {
            return@handler
        }

        val rotation = generateRotation(target, GravityType.from(slot))

        // Set the rotation with the usage priority of 2.
        RotationManager.setRotationTarget(
            rotationConfigurable.toRotationTarget(rotation ?: return@handler, considerInventory = considerInventory),
            Priority.IMPORTANT_FOR_USAGE_2, this
        )
    }

    override fun onDisabled() {
        targetTracker.reset()
    }

    /**
     * Handles the auto shoot logic.
     */
    @Suppress("unused")
    private val handleAutoShoot = tickHandler {
        val target = targetTracker.target ?: return@tickHandler

        if (notDuringCombat && CombatManager.isInCombat) {
            return@tickHandler
        }

        // Check if we have a throwable, if not we can't shoot.
        val slot = getThrowable() ?: return@tickHandler

        if (!trySelect(slot)) {
            return@tickHandler
        }

        val rotation = generateRotation(target, GravityType.from(slot))

        // Check the difference between server and client rotation
        val rotationDifference = RotationManager.serverRotation.angleTo(rotation ?: return@tickHandler)

        // Check if we are not aiming at the target yet
        if (rotationDifference > aimOffThreshold) {
            return@tickHandler
        }

        // Check if we are still aiming at the target
        clicker.click {
            if (player.isUsingItem || (considerInventory && InventoryManager.isInventoryOpen)) {
                return@click false
            }

            interaction.interactItem(
                player,
                slot.useHand,
                RotationManager.serverRotation.yaw,
                RotationManager.serverRotation.pitch
            ).isAccepted
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val target = targetTracker.target ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }

    private fun generateRotation(target: LivingEntity, gravityType: GravityType): Rotation? {
        val pointOnHitbox = pointTracker.gatherPoint(target, PointTracker.AimSituation.FOR_NEXT_TICK)

        return when (gravityType) {
            GravityType.AUTO -> {
                // Should not happen, we convert [gravityType] to LINEAR or PROJECTILE before.
                return null
            }
            GravityType.LINEAR -> Rotation.lookingAt(pointOnHitbox.toPoint, pointOnHitbox.fromPoint)
            // Determines the required yaw and pitch angles to hit a target with a projectile,
            // considering gravity's effect on the projectile's motion.
            GravityType.PROJECTILE -> {
                SituationalProjectileAngleCalculator.calculateAngleForEntity(TrajectoryInfo.GENERIC, target)
            }
        }
    }

    private fun getThrowable(): HotbarItemSlot? {
        return when (throwableType) {
            ThrowableType.EGG_AND_SNOWBALL -> Slots.OffhandWithHotbar.findClosestSlot(Items.EGG, Items.SNOWBALL)
            ThrowableType.ANYTHING -> when {
                !player.mainHandStack.isNothing() -> Slots.Hotbar[player.inventory.selectedSlot]
                !player.offHandStack.isNothing() -> OffHandSlot
                else -> null
            }
        }
    }

    private enum class ThrowableType(override val choiceName: String) : NamedChoice {
        EGG_AND_SNOWBALL("EggAndSnowball"),
        ANYTHING("Anything"),
    }

    private enum class GravityType(override val choiceName: String) : NamedChoice {

        AUTO("Auto"),
        LINEAR("Linear"),
        PROJECTILE("Projectile");

        companion object {
            fun from(slot: HotbarItemSlot): GravityType =
                from(slot.itemStack.item)

            fun from(item: Item): GravityType {
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
