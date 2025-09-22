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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRequirements
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleStuck
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
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
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.projectile.FishingBobberEntity
import net.minecraft.item.Items
import net.minecraft.util.math.Vec3d
import java.util.function.BooleanSupplier
import java.util.function.Function

/**
 * Auto use fishing rod for combat.
 */
object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {

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
    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")
    private val ignores by multiEnumChoice<Ignore>("Ignore")
    private val holdingItemsForIgnore by items(
        "HoldingItemsForIgnore",
        ReferenceOpenHashSet.of(Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.FIRE_CHARGE, Items.ENDER_PEARL)
    )
    private val targetTracker = tree(TargetTracker(TargetPriority.DISTANCE))
    private val pointTracker = tree(PointTracker(this))

    private val rotationConfigurable = tree(RotationsConfigurable(this))
    private val aimOffThreshold by float("AimOffThreshold", 5f, 2f..10f)

    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)

    private val targetRenderer = tree(WorldTargetRenderer(this))

    private val hitTimeout by int("HitTimeout", 30, 5..200, "ticks")
    private val pullOnOutOfRange by boolean("PullOnOutOfRange", true)
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val cooldown by intRange("Cooldown", 4..8, 1..50, "ticks")

    private val requirementsMet
        get() = requires.all { it.asBoolean }
            && !ignores.any { it.asBoolean }
            && player.health > minHealth
            && maxEnemiesNearby == 0 || targetTracker.countTargets() <= maxEnemiesNearby
            && availableRodSlot != null
            && player.mainHandStack.item !in holdingItemsForIgnore
            && !ModuleBlink.running
            && !ModuleScaffold.running
            && !ModuleStuck.running

    private var fishingBobberEntity by computedOn<GameTickEvent, FishingBobberEntity?>(
        priority = FIRST_PRIORITY,
        initialValue = null,
    ) { _, _ ->
        world.entities.firstOrNull { entity ->
            entity is FishingBobberEntity && entity.playerOwner === player
        } as FishingBobberEntity?
    }

    private var availableRodSlot by computedOn<GameTickEvent, HotbarItemSlot?>(
        priority = FIRST_PRIORITY,
        initialValue = null,
    ) { _, old ->
        old?.takeIf {
            it.isSelected && it.itemStack.isOf(Items.FISHING_ROD)
        } ?: Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD)
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (!requirementsMet) {
            targetTracker.reset()
            return@handler
        }

        val maxRangeSq = (range.endInclusive + currentScanExtraRange).sq()
        val minRangeSq = range.start.sq()

        val target = targetTracker.selectFirst { enemy ->
            player.squaredDistanceTo(enemy) in minRangeSq..maxRangeSq && player.canSee(enemy)
                && enemy.getActualHealth() > minTargetHealth
        } ?: return@handler

        val rotation = gravityType.apply(target) ?: return@handler
        RotationManager.setRotationTarget(
            rotationConfigurable.toRotationTarget(rotation, considerInventory = false),
            Priority.IMPORTANT_FOR_USAGE_1,
            this
        )
    }

    @Suppress("unused")
    private val handleAutoRod = tickHandler {
        debugParameter("fishingBobberEntity.hookedEntity") { fishingBobberEntity?.hookedEntity }

        if (!requirementsMet) {
            return@tickHandler
        }

        val slot = availableRodSlot ?: return@tickHandler

        val target = targetTracker.target ?: return@tickHandler

        val rotation = gravityType.apply(target) ?: return@tickHandler
        val rotationDifference = RotationManager.serverRotation.angleTo(rotation)
        if (rotationDifference > aimOffThreshold) return@tickHandler

        // If the player used rod manually, skip use
        if (fishingBobberEntity == null) {
            // 1. select rod
            // 2. push
            if (!useHotbarSlotOrOffhand(
                    slot,
                    // The player should hold the rod util pulling
                    ticksUntilReset = slotResetDelay.last + hitTimeout,
                    swingMode = swingMode
                ).isAccepted
            ) {
                // Action failed
                return@tickHandler
            }
            currentScanExtraRange = scanExtraRange.random()
        }

        val maxRangeSq = (range.endInclusive + currentScanExtraRange).sq()
        val minRangeSq = range.start.sq()

        // 3. timeout / hit entity / no movement / out of range
        waitConditional(hitTimeout) {
            fishingBobberEntity?.hookedEntity != null ||
                fishingBobberEntity?.movement == Vec3d.ZERO ||
                pullOnOutOfRange && player.squaredDistanceTo(target) !in minRangeSq..maxRangeSq
        }

        // 4. pull
        // 5. reset slot (this ticksUntilReset will override prev action)
        useHotbarSlotOrOffhand(slot, slotResetDelay.random(), swingMode = swingMode)

        waitTicks(cooldown.random())
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val target = targetTracker.target ?: return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }

    override fun onDisabled() {
        targetTracker.reset()
        fishingBobberEntity?.let {
            interaction.stopUsingItem(player)
            fishingBobberEntity = null
        }
        availableRodSlot = null
        SilentHotbar.resetSlot(this)
    }

    private enum class GravityType(override val choiceName: String) : NamedChoice, Function<LivingEntity, Rotation?> {
        LINEAR("Linear"),
        PROJECTILE("Projectile");

        override fun apply(target: LivingEntity): Rotation? = when (this) {
            LINEAR -> {
                val eyes = player.eyePos
                val point = pointTracker.findPoint(eyes, target, 1)
                Rotation.lookingAt(point.pos, eyes)
            }

            PROJECTILE -> {
                SituationalProjectileAngleCalculator.calculateAngleForEntity(
                    TrajectoryInfo.FISHING_ROD, target
                )
            }
        }
    }

    private enum class Ignore(override val choiceName: String) : NamedChoice, BooleanSupplier {
        OPEN_INVENTORY("OpenInventory"),
        USING_ITEM("UsingItem"),
        HOLDING_CONSUMABLE("HoldingConsumable");

        override fun getAsBoolean(): Boolean = when (this) {
            OPEN_INVENTORY -> InventoryManager.isInventoryOpen || mc.currentScreen is HandledScreen<*>
            USING_ITEM -> player.isUsingItem
            HOLDING_CONSUMABLE -> player.mainHandStack.isConsumable || player.offHandStack.isConsumable
        }
    }

}
