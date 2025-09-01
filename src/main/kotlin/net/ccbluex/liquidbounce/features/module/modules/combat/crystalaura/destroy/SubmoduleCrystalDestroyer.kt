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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.CrystalAuraSpeedDebugger
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.SubmoduleSetDead
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.math.isHitByLine
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.max

object SubmoduleCrystalDestroyer : ToggleableConfigurable(ModuleCrystalAura, "Destroy", true) {

    val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)
    private val delay by int("Delay", 0, 0..1000, "ms")
    val range by float("Range", 4.5f, 1f..6f)
    val wallsRange by float("WallsRange", 4.5f, 0f..6f)

    // prioritizes faces that are visible, might make the crystal aura slower
    private val prioritizeVisibleFaces by boolean("PrioritizeVisibleFaces", false)

    var postAttackHandlers = arrayOf(CrystalAuraSpeedDebugger, SubmoduleSetDead.CrystalTracker)
    val chronometer = Chronometer()

    fun tick(providedCrystal: EndCrystalEntity? = null) {
        if (!enabled || !chronometer.hasAtLeastElapsed(delay.toLong())) {
            return
        }

        // update the target / validate the provided crystal
        providedCrystal?.let { crystal ->
            // just check if it works, not if it's the best
            CrystalAuraDestroyTargetFactory.validateAndUpdateTarget(crystal)
        } ?: run {
            CrystalAuraDestroyTargetFactory.updateTarget()
        }

        val target = CrystalAuraDestroyTargetFactory.currentTarget ?: return

        val base = FULL_BOX.offset(target.blockPos.down())
        mc.execute {
            ModuleDebug.debugGeometry(
                ModuleCrystalAura,
                "predictedBlock",
                ModuleDebug.DebuggedBox(base, Color4b.GREEN.fade(0.4f))
            )
        }

        val eyePos = player.eyePos

        // find the best spot (and skip if no spot was found)
        val (rotation, vec3d) =
            raytraceBox(
                eyePos,
                target.boundingBox,
                range = range.toDouble(),
                wallsRange = wallsRange.toDouble(),
                futureTarget = base,
                prioritizeVisible = prioritizeVisibleFaces
            ) ?: return

        queueDestroy(rotation, target, base, eyePos, vec3d)
    }

    private fun queueDestroy(rotation: Rotation, target: EndCrystalEntity, base: Box, eyePos: Vec3d, vec3d: Vec3d) {
        // create the action chain to execute
        val action = {
            ModuleCrystalAura.rotationMode.activeChoice.rotate(rotation, isFinished = {
                facingEnemy(
                    toEntity = target,
                    rotation = RotationManager.serverRotation,
                    range = range.toDouble(),
                    wallsRange = wallsRange.toDouble()
                )
            }, onFinished = {
                if (!chronometer.hasAtLeastElapsed(delay.toLong())) {
                    return@rotate
                }

                val target1 = CrystalAuraDestroyTargetFactory.currentTarget ?: return@rotate

                target1.attack(swingMode)
                postAttackHandlers.forEach { it.attacked(target1.id) }
                chronometer.reset()
            })
        }

        // fixes swinging off thread as other packets might interrupt
        if (swingMode.serverSwing && !base.isHitByLine(eyePos, vec3d)) {
            mc.execute(action)
        } else {
            action()
        }
    }

    fun getMaxRange() = max(range, wallsRange)

}
