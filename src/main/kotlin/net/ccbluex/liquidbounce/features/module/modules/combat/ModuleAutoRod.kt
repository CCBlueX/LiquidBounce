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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRequirements
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleStuck
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items

object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {

    private val rotationMode by enumChoice("RotationMode", RotationMode.LINEAR)
    private val range by floatRange("Range", 3.5f..5f, 3f..10f)
    private val scanExtraRange by floatRange("ScanExtraRange", 0.0f..0.0f, 0.0f..5.0f).onChanged { range ->
        currentScanExtraRange = range.random()
    }
    private var currentScanExtraRange: Float = scanExtraRange.random()
    private val enemiesNearby by int("EnemiesNearby", 1, 1..10)
    private val escapeHealthThreshold by int("EscapeHealthThreshold", 10, 1..20)
    private val pushDelay by int("PushDelay", 100, 50..1000)
    private val pullbackDelay by int("PullbackDelay", 500, 50..1000)
    private val aimOffThreshold by float("AimOffThreshold", 5f, 2f..10f)
    private val tickUntilReset by int("TicksUntilSlotReset", 1, 0..20)
    private val selectSlotAutomatically by boolean("SelectSlotAutomatically", true)

    private val requires by multiEnumChoice<KillAuraRequirements>(
        "Requires",
        KillAuraRequirements.VANILLA_NAME
    )

    private val ignore by multiEnumChoice<Ignore>("Ignore")
    private val holdingItemsForIgnore by items(
        "HoldingItemsForIgnore",
        ReferenceOpenHashSet.of(Items.BOW, Items.CROSSBOW, Items.TRIDENT)
    )
    private val rotationConfigurable = tree(RotationsConfigurable(this))
    private val targetTracker = tree(TargetTracker(TargetPriority.DISTANCE))
    private val pointTracker = tree(PointTracker(this))
    private val targetRenderer = tree(WorldTargetRenderer(this))

    private val requirementsMet
        get() = requires.all { it.asBoolean }

    private val pushTimer = Chronometer()
    private val pullbackTimer = Chronometer()
    private var rodInUse = false

    override val running: Boolean
        get() =
            super.running
                && player.mainHandStack.item !in holdingItemsForIgnore
                && !ModuleBlink.running
                && !ModuleScaffold.running
                && !ModuleStuck.running
                && !(Ignore.USING_ITEM !in ignore && player.isUsingItem)
                && !(Ignore.HOLD_CONSUME !in ignore && player.mainHandStack.isConsumable)
                && !(Ignore.OPEN_INVENTORY !in ignore
                && (InventoryManager.isInventoryOpen || mc.currentScreen is HandledScreen<*>))

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (!requirementsMet) {
            targetTracker.reset()
            return@handler
        }
        val maxRangeSq = (range.endInclusive + currentScanExtraRange).sq()
        val mixRangeSq = range.start.sq()

        val target = targetTracker.selectFirst { enemy ->
            player.squaredDistanceTo(enemy) in mixRangeSq..maxRangeSq && player.canSee(enemy)
        } ?: return@handler

        val slot = Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD) ?: return@handler
        if (!slot.trySelect(ModuleAutoRod, selectSlotAutomatically, tickUntilReset)) return@handler

        val rotation = findRotation(target, rotationMode) ?: return@handler
        RotationManager.setRotationTarget(
            rotationConfigurable.toRotationTarget(rotation, considerInventory = false),
            Priority.IMPORTANT_FOR_USAGE_1,
            this
        )
    }

    @Suppress("unused")
    private val handleAutoRod = handler<GameTickEvent> {
        val target = targetTracker.target ?: return@handler

        val slot = Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD) ?: return@handler
        if (!slot.trySelect(ModuleAutoRod, selectSlotAutomatically, tickUntilReset)) return@handler
        if (rodInUse && pullbackTimer.hasElapsed(pullbackDelay.toLong())) {
            KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)
            rodInUse = false
            pushTimer.reset()
            currentScanExtraRange = scanExtraRange.random()
            return@handler
        }

        if (rodInUse) return@handler

        val enemiesList = targetTracker.targets()
        if (enemiesList.isEmpty() || enemiesList.size > enemiesNearby || player.health <= escapeHealthThreshold) {
            return@handler
        }

        val rotation = findRotation(target, rotationMode) ?: return@handler
        val rotationDifference = RotationManager.serverRotation.angleTo(rotation)
        if (rotationDifference > aimOffThreshold) return@handler

        if (pushTimer.hasElapsed(pushDelay.toLong())) {
            SilentHotbar.selectSlotSilently(this, slot, tickUntilReset)
            interaction.syncSelectedSlot()
            KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)
            rodInUse = true
            pullbackTimer.reset()
        }

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
        rodInUse = false
        interaction.stopUsingItem(player)
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
