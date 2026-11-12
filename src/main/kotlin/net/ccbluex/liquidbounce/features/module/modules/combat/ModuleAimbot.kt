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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRequirements
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebuggedBox
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebuggedPoint
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.aim.Aim
import net.ccbluex.liquidbounce.utils.aiming.aim.ConstantAim
import net.ccbluex.liquidbounce.utils.aiming.aim.HumanAim
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.preference.LeastDifferencePreference
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
object ModuleAimbot : ClientModule("Aimbot", ModuleCategories.COMBAT, aliases = listOf("AimAssist", "AutoAim")) {

    private val range = float("Range", 4.2f, 1f..8f)

    val targetTracker = tree(TargetTracker(TargetPriority.DIRECTION, range = range))

    init {
        tree(TargetRenderer(this, targetTracker))
    }
    private val pointTracker = tree(PointTracker(this))
    private val lazyRotation by boolean("LazyRotation", false)

    /**
     * End crystals, which come before the targets of the [targetTracker].
     */
    private object Crystals : ToggleableValueGroup(this, "Crystals", false) {

        /**
         * Leaves the targets of the [targetTracker] out.
         */
        val only by boolean("Only", false)
        private val onlyWhileUsing by boolean("OnlyWhileUsing", false)

        fun targets(): MutableList<Entity> {
            if (onlyWhileUsing && !mc.options.keyUse.isPressedOnAny) {
                return ArrayList()
            }

            val crystals = world.getEntitiesBoxInRange(player.eyePosition, targetTracker.maxRange.toDouble()) {
                it is EndCrystal && targetTracker.isInFov(it)
            }
            crystals.sortByDescending(RotationUtil::crosshairCosToEntity)
            return crystals
        }

    }

    init {
        tree(Crystals)
    }

    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")

    private val requirementsMet
        get() = mc.gui.screen() == null && requires.all { it.asBoolean }

    private val aim = modes<Aim>(this, "Aim", listOf("AngleSmooth"), { 0 }) {
        arrayOf(HumanAim(it), ConstantAim(it))
    }

    private val axis by multiEnumChoice<Axis>("Axis", Axis.HORIZONTAL, Axis.VERTICAL)

    private val ignores by multiEnumChoice<IgnoreOpened>("Ignore")

    @Suppress("unused")
    private val tickHandler = handler<RotationUpdateEvent> { _ ->
        if (!requirementsMet) {
            targetTracker.reset()
            return@handler
        }

        val target = findNextTargetRotation()
        ModuleAutoWeapon.onTarget(targetTracker.target)

        val (entity, rotation) = target ?: return@handler
        val yaw = rotation.rotation.yaw.takeIf { Axis.HORIZONTAL in axis }
        val pitch = rotation.rotation.pitch.takeIf { Axis.VERTICAL in axis }
        if (yaw == null && pitch == null) {
            return@handler
        }

        if (IgnoreOpened.SCREEN !in ignores && mc.gui.screen() != null) {
            return@handler
        }

        // ChangeLook: an aim assist turns the camera itself, and the mouse keeps turning it too
        RotationManager.setRotationTarget(
            RotationTarget(
                yaw = yaw,
                pitch = pitch,
                entity = entity,
                aim = aim.activeMode,
                considerInventory = IgnoreOpened.CONTAINER !in ignores,
                movementCorrection = MovementCorrection.CHANGE_LOOK,
            ),
            Priority.IMPORTANT_FOR_USAGE_1,
            this
        )
    }

    override fun onDisabled() {
        targetTracker.reset()
    }

    private fun findNextTargetRotation(): Pair<Entity, RotationWithVector>? {
        for (entity in targets()) {
            if (lazyRotation) {
                val currentRotation = player.rotation
                val currentHit = isLookingAtEntity(
                    fromEntity = player,
                    toEntity = entity,
                    rotation = currentRotation,
                    range = targetTracker.maxRange.toDouble(),
                    throughWallsRange = 0.0,
                )

                if (currentHit != null) {
                    targetTracker.target = entity as? LivingEntity
                    return entity to RotationWithVector(currentRotation, currentHit.location)
                }
            }

            val eyes = player.eyePosition
            val point = pointTracker.findPoint(eyes, entity)

            debugGeometry("Box") { DebuggedBox(point.box, Color4b.ORANGE.with(a = 90)) }
            debugGeometry("Point") { DebuggedPoint(point.pos, Color4b.WHITE, size = 0.1) }

            val rotationPreference = LeastDifferencePreference.leastDifferenceToLastPoint(eyes, point.pos)
            val rotation = raytraceBox(
                eyes = eyes,
                box = point.box,
                range = targetTracker.maxRange.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: continue

            targetTracker.target = entity as? LivingEntity
            return entity to rotation
        }

        targetTracker.reset()
        return null
    }

    private fun targets(): List<Entity> {
        if (!Crystals.enabled) {
            return targetTracker.targets()
        }

        val targets = Crystals.targets()
        if (!Crystals.only) {
            targets += targetTracker.targets()
        }
        return targets
    }

    private enum class IgnoreOpened(
        override val tag: String
    ) : Tagged {
        SCREEN("Screen"),
        CONTAINER("Container")
    }

    private enum class Axis(override val tag: String) : Tagged {
        HORIZONTAL("Horizontal"),
        VERTICAL("Vertical")
    }
}
