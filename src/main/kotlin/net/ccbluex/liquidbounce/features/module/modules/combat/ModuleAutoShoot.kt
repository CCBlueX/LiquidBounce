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

package net.ccbluex.liquidbounce.features.module.modules.combat

import kotlinx.coroutines.Job
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.launchSequence
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.entity.useItem
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.SnowballItem

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
object ModuleAutoShoot : ClientModule("AutoShoot", ModuleCategories.COMBAT) {

    private val throwableType = choices(
        "ThrowableType",
        EggAndSnowball,
        arrayOf(EggAndSnowball, Custom),
    )
    private val gravityType by enumChoice("GravityType", GravityType.AUTO).apply { tagBy(this) }

    private val clicker = tree(Clicker(this, mc.options.keyUse, itemCooldown = null))

    /**
     * The target tracker to find the best enemy to attack.
     */
    internal val targetTracker = tree(TargetTracker(TargetPriority.DISTANCE, floatRange("Range", 3.0f..6f, 1f..256f)))
    private val pointTracker = tree(
        PointTracker(
            this
        )
    )

    /**
     * So far, I have never seen an anti-cheat which detects high turning speed for actions such as
     * shooting.
     */
    private val rotations = tree(RotationsValueGroup(this))
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f)

    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)

    /**
     * The target renderer to render the target, which we are currently aiming at.
     */
    init {
        tree(TargetRenderer(this, targetTracker))
    }

    private val selectSlotAutomatically by boolean("SelectSlotAutomatically", true)
    private val tickUntilReset by int("TicksUntilSlotReset", 1, 0..20)
    private val considerInventory by boolean("ConsiderInventory", true)

    private val requiresKillAura by boolean("RequiresKillAura", false)
    private val notDuringCombat by boolean("NotDuringCombat", false)
    val constantLag by boolean("ConstantLag", false)

    private val HotbarItemSlot.isSelectionNeeded: Boolean
        get() = hotbarIndex != null && hotbarIndex != SilentHotbar.serversideSlot

    private fun HotbarItemSlot.trySelect(silentHotbarRequester: Any?, select: Boolean, tickUntilReset: Int): Boolean {
        // Select the slot if we are not holding it.
        if (isSelectionNeeded) {
            if (!select) return false
            // If we are not holding the slot, we can't shoot.
            SilentHotbar.selectSlotSilently(silentHotbarRequester, this, tickUntilReset)
            if (isSelectionNeeded) return false
        }
        return true
    }

    private fun shouldPauseForKillAura(): Boolean {
        if (requiresKillAura && !ModuleKillAura.running) {
            targetTracker.reset()
            return true
        }

        return false
    }

    private fun getThrowableSlot(): HotbarItemSlot? {
        val slot = throwableType.activeMode.findSlot() ?: return null

        return slot.takeIf {
            it.trySelect(ModuleAutoShoot, selectSlotAutomatically, tickUntilReset)
        }
    }

    private fun getRotation(target: LivingEntity, slot: HotbarItemSlot): Pair<Rotation, Surface?>? {
        return GravityType.from(slot).rotationFor(target)
    }

    /**
     * The rotation to throw with at the target, planned once per tick.
     */
    private var throwing: Pair<Rotation, Surface?>? = null
    private var shooting: Job? = null

    /**
     * Simulates the next tick, which we use to figure out the required rotation for the next tick to react
     * as fast possible. This means we already pre-aim before we peek around the corner.
     */
    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        throwing = null

        if (shouldPauseForKillAura()) return@handler

        // Find the recommended target
        // Check if we can see the enemy
        val target = targetTracker.selectFirst(player::hasLineOfSight) ?: return@handler

        if (notDuringCombat && CombatManager.isInCombat) {
            return@handler
        }

        // Check if we have a throwable, if not we can't shoot.
        val slot = getThrowableSlot() ?: return@handler
        throwing = getRotation(target, slot) ?: return@handler
        shooting = launchSequence(shooting) { shoot() }
    }

    override fun onDisabled() {
        targetTracker.reset()
        SilentHotbar.resetSlot(ModuleAutoShoot)
    }

    /**
     * Shoots once the rotation going out points within [aimOffThreshold] of the target.
     */
    private suspend fun shoot() {
        val planned by requires { throwing }

        // Set the rotation with the usage priority of 2.
        lookAt(Priority.IMPORTANT_FOR_USAGE_2, {
            rotations.toRotationTarget(
                planned.first,
                considerInventory = considerInventory,
                surface = planned.second,
            )
        }) { going -> going.takeIf { it.directionAngleTo(planned.first) <= aimOffThreshold } }

        // Check if we have a throwable, if not we can't shoot.
        val slot = getThrowableSlot() ?: return

        clicker.click {
            if (player.isUsingItem || (considerInventory && InventoryManager.isInventoryOpen)) {
                return@click false
            }

            useItem(
                slot.useHand,
                swingMode = swingMode,
            ).consumesAction()
        }
    }

    private sealed class ThrowableTypeMode(
        name: String,
        aliases: List<String> = emptyList(),
    ) : Mode(name, aliases) {
        final override val parent: ModeValueGroup<*>
            get() = throwableType

        abstract fun findSlot(): HotbarItemSlot?
    }

    private object EggAndSnowball : ThrowableTypeMode("EggAndSnowball") {
        override fun findSlot(): HotbarItemSlot? = Slots.OffhandWithHotbar.findClosestSlot {
            it.item is EggItem || it.item is SnowballItem
        }
    }

    private object Custom : ThrowableTypeMode("Custom", aliases = listOf("Anything")) {
        private val filter by enumChoice("Filter", Filter.WHITELIST)
        private val items by items("Items", itemSortedSetOf(Items.EGG, Items.SNOWBALL))

        override fun findSlot(): HotbarItemSlot? = Slots.OffhandWithHotbar.findClosestSlot {
            !it.isEmpty && filter(it.item, items)
        }
    }

    private enum class GravityType(override val tag: String) : Tagged {
        AUTO("Auto") {
            override fun rotationFor(target: LivingEntity): Pair<Rotation, Surface?>? = null
        },
        LINEAR("Linear") {
            override fun rotationFor(target: LivingEntity): Pair<Rotation, Surface?> {
                // On linear we likely don't need to care about gravity,
                // but instead aim exactly at the hitbox of the target.
                val eyes = player.eyePosition
                val point = pointTracker.findPoint(eyes, target, 1)
                return Rotation.lookingAt(point.pos, eyes) to Surface(point.box)
            }
        },
        PROJECTILE("Projectile") {
            override fun rotationFor(target: LivingEntity): Pair<Rotation, Surface?>? {
                return SituationalProjectileAngleCalculator.calculateAngleForEntity(
                    TrajectoryInfo.GENERIC,
                    target
                )?.let { it to null }
            }
        };

        /** The rotation to throw with, and the box it meets when the throw flies straight. */
        abstract fun rotationFor(target: LivingEntity): Pair<Rotation, Surface?>?

        companion {
            fun from(slot: HotbarItemSlot): GravityType =
                from(slot.itemStack.item)

            fun from(item: Item): GravityType {
                return when (gravityType) {
                    AUTO -> {
                        when (item) {
                            is EggItem, is SnowballItem -> PROJECTILE
                            else -> LINEAR
                        }
                    }

                    else -> gravityType
                }
            }
        }
    }

}
