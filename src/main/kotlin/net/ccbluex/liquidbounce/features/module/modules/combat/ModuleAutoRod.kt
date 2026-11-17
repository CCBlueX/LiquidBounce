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
import net.ccbluex.fastutil.complement
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.launchSequence
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.event.tickConditional
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRequirements
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.network.releaseUsingItemInTickLoop
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.world.entityGetter
import net.ccbluex.liquidbounce.utils.world.firstOrNull
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import java.util.function.BooleanSupplier

/**
 * Auto use fishing rod for combat.
 */
object ModuleAutoRod : ClientModule("AutoRod", ModuleCategories.COMBAT) {

    private val gravityType by enumChoice("GravityType", GravityType.LINEAR)
    private val range by floatRange("Range", 3.5f..5f, 2f..10f)
    private val scanExtraRange by floatRange("ScanExtraRange", 0.0f..0.0f, 0.0f..5.0f).onChanged { range ->
        currentScanExtraRange = range.random()
    }
    private var currentScanExtraRange: Float = scanExtraRange.random()

    // Requirements
    private val maxEnemiesNearby by int("MaxEnemiesNearby", 1, 0..10) // 0 = no limit
    private val minHealth by float("MinHealth", 10f, 1f..20f)
    private val minTargetHealth by float("MinTargetHealth", 4f, 1f..20f)
    private val requirements by multiEnumChoice<KillAuraRequirements>(
        "Requires",
        choices = enumSetOf(KillAuraRequirements.EMPTY_HAND).complement()
    )
    private val ignores by multiEnumChoice<Ignore>("Ignore")
    private val holdingItemsForIgnore by items(
        "HoldingItemsForIgnore",
        itemSortedSetOf(Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.FIRE_CHARGE, Items.ENDER_PEARL)
    )
    private val targetTracker = tree(TargetTracker(TargetPriority.DISTANCE))
    private val pointTracker = tree(PointTracker(this))

    private val rotations = tree(RotationsValueGroup(this))
    private val aimOffThreshold by float("AimOffThreshold", 5f, 2f..10f)

    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)

    init {
        tree(TargetRenderer(this, targetTracker))
    }

    private val hitTimeout by int("HitTimeout", 30, 5..200, "ticks")
    private val pullOnOutOfRange by boolean("PullOnOutOfRange", true)
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val cooldown by intRange("Cooldown", 4..8, 1..50, "ticks")

    private val requirementsMet
        get() = requirements.all { it.asBoolean }
            && !ignores.any { it.asBoolean }
            && player.health > minHealth
            && availableRodSlot != null
            && player.mainHandItem.item !in holdingItemsForIgnore
            && !ModuleBlink.running
            && !ModuleScaffold.running
            && !ModuleFreeze.running
            && (maxEnemiesNearby == 0 || targetTracker.countTargets() <= maxEnemiesNearby)

    private var fishingBobberEntity by computedOn<GameTickEvent, FishingHook?>(
        priority = FIRST_PRIORITY,
        initialValue = null,
    ) { _, _ ->
        world.entityGetter.firstOrNull(EntityTypes.FISHING_BOBBER) {
            it.playerOwner === player
        }
    }

    private var availableRodSlot by computedOn<GameTickEvent, HotbarItemSlot?>(
        priority = FIRST_PRIORITY,
        initialValue = null,
    ) { _, old ->
        old?.takeIf {
            it.isSelected && it.itemStack.`is`(Items.FISHING_ROD)
        } ?: Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD)
    }

    /**
     * The rotation to throw with at the target, planned once per tick.
     */
    private var throwing: Pair<Rotation, Surface?>? = null
    private var rodUse: Job? = null

    /** Whether our hook is out and still has to be pulled. */
    private var thrown = false

    /** Ticks of [cooldown] left after the last pull before the next throw. */
    private var cooldownLeft = 0

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        debugParameter("fishingBobberEntity.hookedEntity") { fishingBobberEntity?.hookedIn }
        throwing = null

        if (!requirementsMet) {
            targetTracker.reset()
        } else {
            planThrow()
        }

        if (thrown) {
            rodUse = launchSequence(rodUse) { pullRod() }
        } else if (cooldownLeft > 0) {
            cooldownLeft--
        } else if (throwing != null) {
            // A cast of the player's own is pulled like one of ours
            val manualCast = fishingBobberEntity != null
            rodUse = launchSequence(rodUse) { if (manualCast) pullRod() else throwRod() }
        }
    }

    private fun planThrow() {
        val maxRangeSq = (range.endInclusive + currentScanExtraRange).sq()
        val minRangeSq = range.start.sq()

        val target = targetTracker.selectFirst { enemy ->
            player.distanceToSqr(enemy) in minRangeSq..maxRangeSq && player.hasLineOfSight(enemy)
                && enemy.getActualHealth() > minTargetHealth
        } ?: return

        val (rotation, surface) = gravityType.apply(target)?.also { throwing = it } ?: return

        // Keeps aiming while the hook flies and during the cooldown
        RotationManager.setRotationTarget(
            rotations.toRotationTarget(rotation, considerInventory = false, surface = surface),
            Priority.IMPORTANT_FOR_USAGE_1,
            this
        )
    }

    private suspend fun throwRod() {
        val planned by requires { throwing }
        val slot by requires { availableRodSlot }

        lookAt(Priority.IMPORTANT_FOR_USAGE_1, {
            val (rotation, surface) = planned
            rotations.toRotationTarget(rotation, considerInventory = false, surface = surface)
        }) { going -> going.takeIf { it.directionAngleTo(planned.first) <= aimOffThreshold } }

        // 1. select rod
        // 2. push, holding the rod until the pull
        if (useHotbarSlotOrOffhand(slot, slotResetDelay.last + hitTimeout, swingMode = swingMode).consumesAction()) {
            thrown = true
            currentScanExtraRange = scanExtraRange.random()
        }
    }

    private suspend fun pullRod() {
        val slot by requires { availableRodSlot }
        val target = targetTracker.target
        val maxRangeSq = (range.endInclusive + currentScanExtraRange).sq()
        val minRangeSq = range.start.sq()

        // 3. timeout / hit entity / no movement / out of range
        tickConditional(hitTimeout) {
            val hook = fishingBobberEntity
            hook?.hookedIn != null || hook?.knownMovement == Vec3.ZERO || pullOnOutOfRange &&
                (target == null || player.distanceToSqr(target) !in minRangeSq..maxRangeSq)
        }

        // 4. pull
        // 5. reset slot (this ticksUntilReset will override prev action)
        if (fishingBobberEntity != null) {
            useHotbarSlotOrOffhand(slot, slotResetDelay.random(), swingMode = swingMode)
        }
        thrown = false
        cooldownLeft = cooldown.random()
    }

    override fun onDisabled() {
        thrown = false
        cooldownLeft = 0
        targetTracker.reset()
        fishingBobberEntity?.let {
            interaction.releaseUsingItemInTickLoop()
            fishingBobberEntity = null
        }
        availableRodSlot = null
        SilentHotbar.resetSlot(this)
    }

    private enum class GravityType(override val tag: String) : Tagged {
        LINEAR("Linear"),
        PROJECTILE("Projectile");

        /** The rotation to throw the rod with, and the box it meets when the throw flies straight. */
        fun apply(target: LivingEntity): Pair<Rotation, Surface?>? = when (this) {
            LINEAR -> {
                val eyes = player.eyePosition
                val point = pointTracker.findPoint(eyes, target, 1)
                Rotation.lookingAt(point.pos, eyes) to Surface(point.box)
            }

            PROJECTILE -> {
                SituationalProjectileAngleCalculator.calculateAngleForEntity(
                    TrajectoryInfo.FISHING_ROD, target
                )?.let { it to null }
            }
        }
    }

    private enum class Ignore(override val tag: String) : Tagged, BooleanSupplier {
        OPEN_INVENTORY("OpenInventory"),
        USING_ITEM("UsingItem"),
        HOLDING_CONSUMABLE("HoldingConsumable");

        override fun getAsBoolean(): Boolean = when (this) {
            OPEN_INVENTORY -> InventoryManager.isInventoryOpen || mc.gui.screen() is AbstractContainerScreen<*>
            USING_ITEM -> player.isUsingItem
            HOLDING_CONSUMABLE -> player.mainHandItem.isConsumable || player.offhandItem.isConsumable
        }
    }

}
