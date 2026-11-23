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
package net.ccbluex.liquidbounce.features.module.modules.combat.respawnanchoraura

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SwitchMode
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.NormalRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.VisibilityPredicate
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.isUnobstructed
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
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
import net.ccbluex.liquidbounce.utils.world.respawnAnchorWorks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RespawnAnchorBlock
import net.minecraft.world.phys.HitResult
import kotlin.math.max

/**
 * Places, charges and detonates respawn anchors near the selected target.
 */
object ModuleRespawnAnchorAura : ClientModule(
    "RespawnAnchorAura",
    ModuleCategories.COMBAT,
    aliases = listOf("AnchorAura"),
    disableOnQuit = true
) {

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

    private val placer = tree(BlockPlacer(
        "Placing",
        this,
        Priority.IMPORTANT_FOR_USAGE_2,
        slotFinder = { Slots.OffhandWithHotbar.findClosestSlot(Items.RESPAWN_ANCHOR) }
    ))

    private val rotationMode = modes(this, "RotationMode") {
        arrayOf(
            NormalRotationMode(it, this, Priority.IMPORTANT_FOR_USAGE_2, true),
            NoRotationMode(it, this)
        )
    }

    private val detonateTimer = Chronometer()
    private var placeTarget: BlockPos? = null
    private var detonateTarget: BlockPos? = null
    private var positions = BlockPos.ZERO.getSortedSphere(4.5f)

    init {
        treeAll(Place, Detonate)
        tree(TargetRenderer(this, targetTracker))
    }

    override fun onEnabled() {
        updatePositions()
    }

    override fun onDisabled() {
        targetTracker.reset()
        placeTarget = null
        detonateTarget = null
        placer.disable()
    }

    @Suppress("unused")
    private val tickHandler = handler<RotationUpdateEvent> {
        if (world.respawnAnchorWorks || CombatManager.shouldPauseCombat ||
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
            placer.clear()
            detonateTarget?.let(::detonate)
            return@handler
        }

        placeTarget = if (Place.enabled) findPlaceTarget(target) else null
        placer.update(placeTarget?.let(::setOf).orEmpty())
    }

    private fun findPlaceTarget(target: LivingEntity): BlockPos? {
        if (Slots.OffhandWithHotbar.findClosestSlot(Items.RESPAWN_ANCHOR) == null ||
            Slots.OffhandWithHotbar.findClosestSlot(Items.GLOWSTONE) == null
        ) {
            return null
        }

        val playerPos = player.blockPosition()
        return positions.asSequence()
            .map { playerPos.offset(it) }
            .filter { pos ->
                val state = pos.stateOrEmpty
                state.canBeReplaced() && pos.isUnobstructed() &&
                    DirectionSupport.hasSupport(pos) && isSafe(pos, target) &&
                    raytraceBox(
                        player.eyePosition,
                        FULL_BOX.move(pos),
                        Place.range.toDouble(),
                        Place.wallsRange.toDouble(),
                        VisibilityPredicate.Block(pos, null)
                    ) != null
            }
            .maxByOrNull { target.getDamageFromExplosion(it.center, maxBlastResistance = blastResistance) }
    }

    private fun findDetonateTarget(target: LivingEntity): BlockPos? {
        val playerPos = player.blockPosition()
        return positions.asSequence()
            .map { playerPos.offset(it) }
            .filter { pos ->
                pos.stateOrEmpty.block == Blocks.RESPAWN_ANCHOR && isSafe(pos, target) &&
                    raytraceBox(
                        player.eyePosition,
                        FULL_BOX.move(pos),
                        Detonate.range.toDouble(),
                        Detonate.wallsRange.toDouble(),
                        VisibilityPredicate.Block(pos, null)
                    ) != null
            }
            .maxByOrNull { target.getDamageFromExplosion(it.center, maxBlastResistance = blastResistance) }
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

    private fun detonate(pos: BlockPos) {
        val rotation = raytraceBox(
            player.eyePosition,
            FULL_BOX.move(pos),
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

            val state = pos.stateOrEmpty
            if (state.block != Blocks.RESPAWN_ANCHOR || !isSafe(pos, targetTracker.target ?: return@rotate)) {
                return@rotate
            }

            val charged = state.getValue(RespawnAnchorBlock.CHARGE) > 0
            val slot = if (charged) {
                Slots.OffhandWithHotbar.findClosestSlot { it.item != Items.GLOWSTONE }
            } else {
                Slots.OffhandWithHotbar.findClosestSlot(Items.GLOWSTONE)
            } ?: return@rotate

            val hit = raytraceBlock(
                max(Detonate.range, Detonate.wallsRange).toDouble(),
                RotationManager.serverRotation,
                pos,
                state
            )?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == pos } ?: return@rotate

            player.clickBlockWithSlot(hit, slot.inventorySlot, swingMode, switchMode)
            detonateTimer.reset()
            detonateTarget = null
        })
    }

    private fun clearTargets() {
        targetTracker.reset()
        placeTarget = null
        detonateTarget = null
        placer.clear()
    }

    private fun updatePositions() {
        val range = max(
            max(Place.range, Place.wallsRange),
            max(Detonate.range, Detonate.wallsRange)
        )
        positions = BlockPos.ZERO.getSortedSphere(range)
    }

    private val blastResistance: Float?
        get() = if (terrain) 9f else null

    private object DirectionSupport {
        fun hasSupport(pos: BlockPos): Boolean = Direction.entries.any {
            !pos.relative(it).stateOrEmpty.canBeReplaced()
        }
    }

}
