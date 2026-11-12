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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy

import kotlinx.coroutines.Job
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.guarded
import net.ccbluex.liquidbounce.event.launchSequence
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.CrystalAuraSpeedDebugger
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.SubmoduleSetDead
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebuggedBox
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugGeometry
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.EntityHitResult
import kotlin.math.max

object SubmoduleCrystalDestroyer : ToggleableValueGroup(ModuleCrystalAura, "Destroy", true) {

    val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)
    private val delay by int("Delay", 0, 0..1000, "ms")
    val range by float("Range", 4.5f, 1f..6f)
    val wallsRange by float("WallsRange", 4.5f, 0f..6f)

    // prioritizes faces that are visible, might make the crystal aura slower
    private val prioritizeVisibleFaces by boolean("PrioritizeVisibleFaces", false)

    var postAttackHandlers = arrayOf(CrystalAuraSpeedDebugger, SubmoduleSetDead.CrystalTracker)
    val chronometer = Chronometer()

    /**
     * A crystal to attack, with the rotation that hits it.
     */
    private class Attack(val rotation: Rotation, val target: EndCrystal)

    /** The latest attack [tick] found, which may be computed off-thread. */
    @Volatile
    private var attack: Attack? = null
    private var attacking: Job? = null
    private var attackingTarget: EndCrystal? = null

    fun tick(providedCrystal: EndCrystal? = null) {
        val next = findAttack(providedCrystal)
        attack = next
        if (next != null) {
            mc.execute { follow(next) }
        }
    }

    private fun findAttack(providedCrystal: EndCrystal?): Attack? {
        if (!enabled || !chronometer.hasAtLeastElapsed(delay.toLong())) {
            return null
        }

        val crystal = if (ModuleCrystalAura.crosshairOnly) {
            (mc.hitResult as? EntityHitResult)?.entity as? EndCrystal ?: return null
        } else {
            providedCrystal
        }

        // update the target / validate the provided crystal
        crystal?.let {
            // just check if it works, not if it's the best
            CrystalAuraDestroyTargetFactory.validateAndUpdateTarget(it)
        } ?: run {
            CrystalAuraDestroyTargetFactory.updateTarget()
        }

        val target = CrystalAuraDestroyTargetFactory.currentTarget ?: return null

        val base = FULL_BOX.move(target.blockPosition().below())
        mc.execute {
            ModuleCrystalAura.debugGeometry("predictedBlock") { DebuggedBox(base, Color4b.GREEN.fade(0.4f)) }
        }

        // find the best spot (and skip if no spot was found)
        val spot = raytraceBox(
            player.eyePosition,
            target.boundingBox,
            range = range.toDouble(),
            wallsRange = wallsRange.toDouble(),
            futureTarget = base,
            prioritizeVisible = prioritizeVisibleFaces
        ) ?: return null

        return Attack(spot.rotation, target)
    }

    /**
     * Keeps one sequence attacking the current [attack], started over when it moves to another crystal.
     */
    private fun follow(next: Attack) {
        if (attackingTarget !== next.target) {
            attacking?.cancel()
            attackingTarget = next.target
        }

        attacking = launchSequence(attacking) { destroy(next.target) }
    }

    private suspend fun destroy(target: EndCrystal) {
        val surface = Surface(target.boundingBox, throughWalls = wallsRange > 0f)
        guarded {
            val current by tracked { attack?.takeIf { it.target === target } }
            ModuleCrystalAura.rotationMode.activeMode.look({ current.rotation }, surface) { view ->
                isLookingAtEntity(
                    toEntity = target,
                    rotation = view,
                    range = range.toDouble(),
                    throughWallsRange = wallsRange.toDouble()
                )
            } ?: return@guarded

            if (!chronometer.hasAtLeastElapsed(delay.toLong())) {
                return@guarded
            }

            val crystal = CrystalAuraDestroyTargetFactory.currentTarget ?: return@guarded

            attackEntity(crystal, swingMode)
            postAttackHandlers.forEach { it.attacked(crystal.id) }
            chronometer.reset()
        }
    }

    fun getMaxRange() = max(range, wallsRange)

}
