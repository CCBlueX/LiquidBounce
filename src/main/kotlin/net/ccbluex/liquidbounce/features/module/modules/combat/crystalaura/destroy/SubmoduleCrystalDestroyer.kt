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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.CrystalAuraSpeedDebugger
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.SubmoduleSetDead
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebuggedBox
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugGeometry
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.isHitByLine
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

object SubmoduleCrystalDestroyer : ToggleableValueGroup(ModuleCrystalAura, "Destroy", true) {

    val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)
    private val delay by int("Delay", 0, 0..1000, "ms")
    val range by float("Range", 4.5f, 1f..6f)
    val wallsRange by float("WallsRange", 4.5f, 0f..6f)

    // prioritizes faces that are visible, might make the crystal aura slower
    private val prioritizeVisibleFaces by boolean("PrioritizeVisibleFaces", false)

    /**
     * Keeps the yaw and only changes the pitch to break a crystal on the block the last crystal was placed on,
     * if that reaches it.
     */
    private val pitchOnLastBase by boolean("PitchOnlyOnLastBase", false)

    var postAttackHandlers = arrayOf(CrystalAuraSpeedDebugger, SubmoduleSetDead.CrystalTracker)
    val chronometer = Chronometer()

    fun tick(providedCrystal: EndCrystal? = null) {
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

        val base = FULL_BOX.move(target.blockPosition().below())
        mc.execute {
            ModuleCrystalAura.debugGeometry("predictedBlock") { DebuggedBox(base, Color4b.GREEN.fade(0.4f)) }
        }

        val eyePos = player.eyePosition

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

        val onLastBase = pitchOnLastBase && target.blockPosition().below() == SubmoduleCrystalPlacer.lastBase
        val finalRotation = if (onLastBase) {
            // a crystal that is already broken is gone from the world, so no pitch reaches it and turning would only
            // give the yaw away
            pitchOnlyRotation(target) ?: if (target.isAlive) rotation else return
        } else {
            rotation
        }

        queueDestroy(finalRotation, target, base, eyePos, vec3d)
    }

    /**
     * The rotation with the current yaw that looks at the middle of the part of [crystal] the yaw passes through,
     * or `null` if no pitch reaches it.
     */
    private fun pitchOnlyRotation(crystal: EndCrystal): Rotation? {
        val yaw = RotationManager.serverRotation.yaw
        val eyes = player.eyePosition
        val box = crystal.boundingBox

        // the line along the yaw at the height of the crystal's middle, long enough to cross the whole box
        val direction = Vec3.directionFromRotation(0f, yaw)
        val origin = Vec3(eyes.x, box.center.y, eyes.z)
        val reach = direction.scale(getMaxRange() + box.xsize + box.zsize)
        val enter = box.clip(origin.subtract(reach), origin.add(reach)).getOrNull() ?: return null
        val exit = box.clip(origin.add(reach), origin.subtract(reach)).getOrNull() ?: return null

        val near = max(enter.subtract(origin).dot(direction), 0.0)
        val far = exit.subtract(origin).dot(direction)
        if (far <= 0.0) {
            return null
        }

        val aim = origin.add(direction.scale((near + far) / 2.0))
        return Rotation(yaw, Rotation.lookingAt(aim, eyes).pitch).takeIf {
            isLookingAtEntity(
                toEntity = crystal,
                rotation = it,
                range = range.toDouble(),
                throughWallsRange = wallsRange.toDouble()
            ) != null
        }
    }

    private fun queueDestroy(rotation: Rotation, target: EndCrystal, base: AABB, eyePos: Vec3, vec3d: Vec3) {
        // create the action chain to execute
        val action = {
            ModuleCrystalAura.rotationMode.activeMode.rotate(rotation, isFinished = {
                isLookingAtEntity(
                    toEntity = target,
                    rotation = RotationManager.serverRotation,
                    range = range.toDouble(),
                    throughWallsRange = wallsRange.toDouble()
                ) != null
            }, onFinished = {
                if (!chronometer.hasAtLeastElapsed(delay.toLong())) {
                    return@rotate
                }

                val target1 = CrystalAuraDestroyTargetFactory.currentTarget ?: return@rotate

                attackEntity(target1, swingMode)
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

    /**
     * Attacks the crystal on the block the last crystal was placed on, if it is still in range.
     */
    fun breakLastPlaced() {
        val base = SubmoduleCrystalPlacer.lastBase ?: return
        val crystal = world.getEntitiesOfClass(EndCrystal::class.java, AABB(base.above())) { it.isAlive }
            .firstOrNull() ?: return

        if (crystal.squaredBoxedDistanceTo(player.eyePosition) > getMaxRange().sq()) {
            return
        }

        attackEntity(crystal, swingMode)
        postAttackHandlers.forEach { it.attacked(crystal.id) }
    }

    fun getMaxRange() = max(range, wallsRange)

}
