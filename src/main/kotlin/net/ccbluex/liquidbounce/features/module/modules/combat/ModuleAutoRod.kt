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
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.interactItem
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
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
import kotlin.ranges.contains

/**
 * Auto use rod for PvP.
 *
 * Action chain:
 * 1. select rod
 * 2. use (push)
 * 3. timeout/hooked entity
 * 4. use (pull)
 * 5. reset slot
 */
object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {

    private val rotationMode by enumChoice("RotationMode", RotationMode.LINEAR)
    private val range by floatRange("Range", 3.5f..5f, 3f..10f)
    private val scanExtraRange by floatRange("ScanExtraRange", 0.0f..0.0f, 0.0f..5.0f).onChanged { range ->
        currentScanExtraRange = range.random()
    }
    private var currentScanExtraRange: Float = scanExtraRange.random()

    // Requirements
    private val maxEnemiesNearby by int("MaxEnemiesNearby", 1, 1..10)
    private val minHealth by float("MinHealth", 10f, 1f..20f)
    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")
    private val ignore by multiEnumChoice<Ignore>("Ignore")
    private val holdingItemsForIgnore by items(
        "HoldingItemsForIgnore",
        ReferenceOpenHashSet.of(Items.BOW, Items.CROSSBOW, Items.TRIDENT)
    )

    private val hitTimeout by int("HitTimeout", 40, 5..200, "ticks")
    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)
    private val aimOffThreshold by float("AimOffThreshold", 5f, 2f..10f)
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val selectSlotAutomatically by boolean("SelectSlotAutomatically", true)
    private val cooldown by intRange("Cooldown", 8..10, 1..50, "ticks")

    private val rotationConfigurable = tree(RotationsConfigurable(this))
    private val targetTracker = tree(TargetTracker(TargetPriority.DISTANCE))
    private val pointTracker = tree(PointTracker(this))
    private val targetRenderer = tree(WorldTargetRenderer(this))

    private val requirementsMet
        get() = requires.all { it.asBoolean }
            && player.health > minHealth
            && targetTracker.countTargets() > maxEnemiesNearby
            && availableRodSlot != null
            && player.mainHandStack.item !in holdingItemsForIgnore
            && !ModuleBlink.running
            && !ModuleScaffold.running
            && !ModuleStuck.running
            && !(Ignore.USING_ITEM !in ignore && player.isUsingItem)
            && !(Ignore.HOLD_CONSUME !in ignore && (player.mainHandStack.isConsumable || player.offHandStack.isConsumable))
            && !(Ignore.OPEN_INVENTORY !in ignore
            && (InventoryManager.isInventoryOpen || mc.currentScreen is HandledScreen<*>))

    private fun HotbarItemSlot.interactHand() =
        interaction.interactItem(
            player,
            this.useHand,
            RotationManager.serverRotation.yaw,
            RotationManager.serverRotation.pitch
        ).also {
            if (it.isAccepted) {
                swingMode.swing(this.useHand)
            }
        }

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
            (it.isSelected || selectSlotAutomatically) && it.itemStack.isOf(Items.FISHING_ROD)
        } ?: Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD)
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (!requirementsMet) {
            targetTracker.reset()
        }

        val maxRangeSq = (range.endInclusive + currentScanExtraRange).sq()
        val mixRangeSq = range.start.sq()

        val target = targetTracker.selectFirst { enemy ->
            player.squaredDistanceTo(enemy) in mixRangeSq..maxRangeSq && player.canSee(enemy)
        } ?: return@handler

        val rotation = findRotation(target, rotationMode) ?: return@handler
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

        val rotation = findRotation(target, rotationMode) ?: return@tickHandler
        val rotationDifference = RotationManager.serverRotation.angleTo(rotation)
        if (rotationDifference > aimOffThreshold) return@tickHandler

        // 1. select rod
        if (!slot.trySelect(ModuleAutoRod, selectSlotAutomatically, slotResetDelay.last + hitTimeout)) {
            return@tickHandler
        }

        // 2. push
        if (!slot.interactHand().isAccepted) {
            // Action failed
            return@tickHandler
        }
        currentScanExtraRange = scanExtraRange.random()

        // 3. timeout / hit entity
        waitConditional(hitTimeout) {
            fishingBobberEntity?.hookedEntity != null
        }

        // 4. pull + reset slot
        if (slot.trySelect(ModuleAutoRod, selectSlotAutomatically, slotResetDelay.random())) {
            slot.interactHand()
        }

        waitTicks(cooldown.random())
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val target = targetTracker.target ?: return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }

    private fun findRotation(target: LivingEntity, mode: RotationMode): Rotation? {
        return when (mode) {
            RotationMode.LINEAR -> {
                val eyes = player.eyePos
                val point = pointTracker.findPoint(eyes, target, 1)
                Rotation.lookingAt(point.pos, eyes)
            }

            RotationMode.PROJECTILE -> {
                SituationalProjectileAngleCalculator.calculateAngleForEntity(
                    TrajectoryInfo.FISHING_ROD, target
                )
            }
        }
    }

    override fun onDisabled() {
        targetTracker.reset()
        fishingBobberEntity?.let {
            interaction.stopUsingItem(player)
            fishingBobberEntity = null
        }
        availableRodSlot = null
    }

    private enum class RotationMode(override val choiceName: String) : NamedChoice {
        LINEAR("Linear"),
        PROJECTILE("Projectile")
    }

    private enum class Ignore(override val choiceName: String) : NamedChoice {
        OPEN_INVENTORY("OpenInventory"),
        USING_ITEM("UsingItem"),
        HOLD_CONSUME("HoldConsume"),
    }

}
