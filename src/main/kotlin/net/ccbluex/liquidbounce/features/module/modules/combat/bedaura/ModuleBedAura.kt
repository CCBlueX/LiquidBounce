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
package net.ccbluex.liquidbounce.features.module.modules.combat.bedaura

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SwitchMode
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.NormalRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.VisibilityPredicate
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.isBed
import net.ccbluex.liquidbounce.utils.block.isUnobstructed
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.ClickableCenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.center
import net.ccbluex.liquidbounce.utils.network.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.ccbluex.liquidbounce.utils.world.bedRule
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import kotlin.math.max

/**
 * Places and detonates beds in dimensions where using a bed causes an explosion.
 */
object ModuleBedAura : ClientModule("BedAura", ModuleCategories.COMBAT, disableOnQuit = true) {

    private val targetTracker = tree(TargetTracker(
        rangeValue = FloatValueProvider("Range", 4.5f, 1f..12f)
    ))

    private val minDamage by float("MinDamage", 5f, 0f..36f)
    private val maxSelfDamage by float("MaxSelfDamage", 2f, 0f..36f)
    private val maxFriendDamage by float("MaxFriendDamage", 1f, 0f..36f)
    private val antiSuicide by boolean("AntiSuicide", true)
    private val efficient by boolean("Efficient", true)
    private val terrain by boolean("Terrain", true)
    private val notWhileUsingItem by boolean("NotWhileUsingItem", false)

    private object Place : ToggleableValueGroup(this, "Place", true) {
        val delay by int("Delay", 0, 0..1000, "ms")
        val range by float("Range", 4.5f, 1f..6f).onChanged { updatePositions() }
        val wallsRange by float("WallsRange", 4.5f, 0f..6f).onChanged { updatePositions() }
    }

    private object Detonate : ToggleableValueGroup(this, "Detonate", true) {
        val delay by int("Delay", 0, 0..1000, "ms")
        val range by float("Range", 4.5f, 1f..6f).onChanged { updatePositions() }
        val wallsRange by float("WallsRange", 4.5f, 0f..6f).onChanged { updatePositions() }
    }

    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)
    private val switchMode by enumChoice("Switch", SwitchMode.SILENT)

    private val rotationMode = modes(this, "RotationMode") {
        arrayOf(
            NormalRotationMode(it, this, Priority.IMPORTANT_FOR_USAGE_2, true),
            NoRotationMode(it, this)
        )
    }

    private val placeTimer = Chronometer()
    private val detonateTimer = Chronometer()
    private var positions = BlockPos.ZERO.getSortedSphere(4.5f)
    private var placeTarget: BedPlacement? = null
    private var detonateTarget: BlockPos? = null

    init {
        treeAll(Place, Detonate)
        tree(TargetRenderer(this, targetTracker))
    }

    override fun onEnabled() {
        updatePositions()
    }

    override fun onDisabled() {
        clearTargets()
    }

    @Suppress("unused")
    private val tickHandler = handler<RotationUpdateEvent> {
        if (!world.bedRule.destroyOnUse || CombatManager.shouldPauseCombat ||
            notWhileUsingItem && player.isUsingItem
        ) {
            clearTargets()
            return@handler
        }

        targetTracker.selectFirst()
        val target = targetTracker.target ?: run {
            clearTargets()
            return@handler
        }

        detonateTarget = if (Detonate.enabled && detonateTimer.hasAtLeastElapsed(Detonate.delay.toLong())) {
            findDetonateTarget(target)
        } else {
            null
        }

        if (detonateTarget != null) {
            placeTarget = null
            detonateTarget?.let(::detonate)
            return@handler
        }

        placeTarget = if (Place.enabled && placeTimer.hasAtLeastElapsed(Place.delay.toLong())) {
            findPlaceTarget(target)
        } else {
            null
        }
        placeTarget?.let(::place)
    }

    private fun findPlaceTarget(target: LivingEntity): BedPlacement? {
        val slot = Slots.OffhandWithHotbar.findClosestSlot(ItemTags.BEDS) ?: return null
        val playerPos = player.blockPosition()
        var best: BedPlacement? = null

        for (offset in positions) {
            val foot = playerPos.offset(offset)
            if (!foot.stateOrEmpty.canBeReplaced() || !foot.isUnobstructed()) {
                continue
            }

            for (direction in Direction.Plane.HORIZONTAL) {
                val head = foot.relative(direction)
                if (!head.stateOrEmpty.canBeReplaced() || !head.isUnobstructed() ||
                    !isSafe(foot, target) || !isSafe(head, target)
                ) {
                    continue
                }

                val placement = findPlacement(foot, direction, slot.itemStack) ?: continue
                val damage = target.getDamageFromExplosion(foot.center, maxBlastResistance = blastResistance) +
                    target.getDamageFromExplosion(head.center, maxBlastResistance = blastResistance)
                if (best == null || damage > best.damage) {
                    best = BedPlacement(foot, head, direction, placement, slot.inventorySlot, damage)
                }
            }
        }

        return best
    }

    private fun findPlacement(
        foot: BlockPos,
        direction: Direction,
        stack: net.minecraft.world.item.ItemStack
    ): BlockPlacementTarget? {
        val options = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions.Default,
            FaceHandlingOptions(ClickableCenterTargetPositionFactory, Place.wallsRange > 0),
            stack,
            PlayerLocationOnPlacement(rotation = Rotation(direction.toYRot(), 0f)),
            max(Place.range, Place.wallsRange).toDouble()
        )
        return findBestBlockPlacementTarget(foot, options)?.takeIf {
            it.placedBlock == foot && it.verifyClick() != null &&
                it.verifyClick(Rotation(direction.toYRot(), 0f)) != null
        }
    }

    private fun place(plan: BedPlacement) {
        val placementRotation = Rotation(plan.direction.toYRot(), plan.placement.rotation.pitch)
        rotationMode.activeMode.rotate(placementRotation, isFinished = {
            plan.placement.verifyClick(RotationManager.serverRotation) != null
        }, onFinished = {
            if (placeTarget != plan || !placeTimer.hasAtLeastElapsed(Place.delay.toLong())) {
                return@rotate
            }

            if (!plan.foot.stateOrEmpty.canBeReplaced() || !plan.head.stateOrEmpty.canBeReplaced() ||
                !plan.foot.isUnobstructed() || !plan.head.isUnobstructed()
            ) {
                return@rotate
            }

            val target = targetTracker.target ?: return@rotate
            if (!isSafe(plan.foot, target) || !isSafe(plan.head, target)) {
                return@rotate
            }

            val hit = plan.placement.verifyClick(RotationManager.serverRotation) ?: return@rotate
            player.clickBlockWithSlot(hit, plan.slot, swingMode, switchMode)
            placeTimer.reset()
            placeTarget = null
        })
    }

    private fun findDetonateTarget(target: LivingEntity): BlockPos? {
        val playerPos = player.blockPosition()
        return positions.asSequence()
            .map { playerPos.offset(it) }
            .filter { pos ->
                val state = pos.stateOrEmpty
                state.isBed && state.getValue(BedBlock.PART) == BedPart.HEAD &&
                    isSafe(pos, target) && isSafe(pos.relative(state.getValue(BedBlock.FACING).opposite), target) &&
                    raytraceBox(
                        player.eyePosition,
                        bedBox(pos),
                        Detonate.range.toDouble(),
                        Detonate.wallsRange.toDouble(),
                        VisibilityPredicate.Block(pos, null)
                    ) != null
            }
            .maxByOrNull { target.getDamageFromExplosion(it.center, maxBlastResistance = blastResistance) }
    }

    private fun detonate(pos: BlockPos) {
        val rotation = raytraceBox(
            player.eyePosition,
            bedBox(pos),
            Detonate.range.toDouble(),
            Detonate.wallsRange.toDouble(),
            VisibilityPredicate.Block(pos, null)
        ) ?: return

        rotationMode.activeMode.rotate(rotation.rotation, isFinished = {
            raytraceBlock(
                max(Detonate.range, Detonate.wallsRange).toDouble(),
                RotationManager.serverRotation,
                pos,
                pos.stateOrEmpty
            )?.blockPos == pos
        }, onFinished = {
            if (detonateTarget != pos || !detonateTimer.hasAtLeastElapsed(Detonate.delay.toLong())) {
                return@rotate
            }

            val target = targetTracker.target ?: return@rotate
            if (!pos.stateOrEmpty.isBed || !isSafe(pos, target)) {
                return@rotate
            }

            val slot = Slots.OffhandWithHotbar.findClosestSlot { !it.`is`(ItemTags.BEDS) } ?: return@rotate
            val hit = raytraceBlock(
                max(Detonate.range, Detonate.wallsRange).toDouble(),
                RotationManager.serverRotation,
                pos,
                pos.stateOrEmpty
            )?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == pos } ?: return@rotate

            player.clickBlockWithSlot(hit, slot.inventorySlot, swingMode, switchMode)
            detonateTimer.reset()
            detonateTarget = null
        })
    }

    private fun isSafe(pos: BlockPos, target: LivingEntity): Boolean {
        val explosionPos = pos.center
        val resistance = blastResistance
        val damage = target.getDamageFromExplosion(explosionPos, maxBlastResistance = resistance)
        if (damage < minDamage) {
            return false
        }

        val selfDamage = player.getDamageFromExplosion(explosionPos, maxBlastResistance = resistance)
        if (selfDamage > maxSelfDamage ||
            antiSuicide && selfDamage >= player.health + player.absorptionAmount ||
            efficient && damage <= selfDamage
        ) {
            return false
        }

        return maxFriendDamage >= 36f || world.getEntitiesBoxInRange(explosionPos, 6.0) {
            it is LivingEntity && FriendManager.isFriend(it)
        }.none {
            (it as LivingEntity).getDamageFromExplosion(
                explosionPos,
                maxBlastResistance = resistance
            ) > maxFriendDamage
        }
    }

    private fun clearTargets() {
        targetTracker.reset()
        placeTarget = null
        detonateTarget = null
    }

    private fun updatePositions() {
        positions = BlockPos.ZERO.getSortedSphere(max(
            max(Place.range, Place.wallsRange),
            max(Detonate.range, Detonate.wallsRange)
        ))
    }

    private fun bedBox(pos: BlockPos) = AABB(pos).setMaxY(pos.y + 0.5625)

    private val blastResistance: Float?
        get() = if (terrain) 9f else null

    private data class BedPlacement(
        val foot: BlockPos,
        val head: BlockPos,
        val direction: Direction,
        val placement: BlockPlacementTarget,
        val slot: Int,
        val damage: Float
    )

}
