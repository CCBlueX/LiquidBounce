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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * Tries to run calculations with simulated player positions.
 */
abstract class PredictFeature(name: String) : ToggleableValueGroup(ModuleCrystalAura, name, true) {

    /**
     * The ticks should be equal to `20 / cps` to get the approximate time it would take to place a crystal.
     */
    val placeTicks by int("PlaceTicks", 2, 0..20)

    /**
     * Should normally be lower than the place ticks (except when using ID-Predict).
     */
    val destroyTicks by int("BreakTicks", 1, 0..20)

    /**
     * Should be higher than the place ticks. Normally about one to two tick.
     */
    val basePlaceTicks by int("BasePlaceTicks", 4, 0..20)

    /**
     * How the predicted data will be used. For damage prediction only.
     */
    val calculationMode = modes(this, "CalculationMode") {
        arrayOf(Both(it), PredictOnly(it))
    }

    /**
     * Check if the target will block the placement.
     */
    private val checkIntersect by boolean("CheckIntersect", true)

    companion object {
        fun willBeBlocked(box: AABB, target: LivingEntity, basePlace: Boolean): Boolean {
            return SelfPredict.willBeBlocked(box, null, basePlace) ||
                (target is Player && TargetPredict.willBeBlocked(box, target, basePlace))
        }
    }

    fun willBeBlocked(box: AABB, target: Player?, basePlace: Boolean): Boolean {
        if (!enabled || !checkIntersect) {
            return false
        }

        val simulation = getSnapshotPos(target, if (basePlace) basePlaceTicks else placeTicks)

        val boundingBox = target?.boundingBox ?: player.boundingBox
        val halfWidth = abs(boundingBox.maxX - boundingBox.minX) / 2.0
        val predictedBoundingBox = AABB(
            simulation.x - halfWidth,
            simulation.y,
            simulation.z - halfWidth,
            simulation.x + halfWidth,
            simulation.y + boundingBox.maxY - boundingBox.minY,
            simulation.z + halfWidth
        )

        mc.execute {
            ModuleDebug.debugGeometry(
                ModuleCrystalAura,
                "predictedIntersect$name",
                ModuleDebug.DebuggedBox(predictedBoundingBox, Color4b.BLUE.fade(0.4f))
            )
        }

        return box.intersects(predictedBoundingBox)
    }

    fun getDamage(
        player: Player,
        ticks: Int,
        crystal: Vec3,
        maxBlastResistance: Float? = null,
        include: BlockPos? = null
    ): DamageProvider {
        if (!enabled) {
            return NormalDamageProvider(player.getDamageFromExplosion(
                crystal,
                include = include,
                maxBlastResistance = maxBlastResistance
            ))
        }

        val simulated = getSnapshotPos(player, ticks)

        val boundingBox = player.boundingBox
        val halfWidth = abs(boundingBox.maxX - boundingBox.minX) / 2.0
        val predictedBoundingBox = AABB(
            simulated.x - halfWidth,
            simulated.y,
            simulated.z - halfWidth,
            simulated.x + halfWidth,
            simulated.y + boundingBox.maxY - boundingBox.minY,
            simulated.z + halfWidth
        )

        mc.execute {
            ModuleDebug.debugGeometry(
                ModuleCrystalAura,
                "predictedDamage$name",
                ModuleDebug.DebuggedBox(predictedBoundingBox, Color4b.GRAY.fade(0.4f))
            )
        }

        val predictedDamage = player.getDamageFromExplosion(
            crystal,
            include = include,
            maxBlastResistance = maxBlastResistance,
            entityBoundingBox = predictedBoundingBox
        )

        val calcMode = calculationMode.activeMode
        if (calcMode is PredictOnly) {
            return NormalDamageProvider(predictedDamage)
        }

        val damage = player.getDamageFromExplosion(crystal, include = include, maxBlastResistance = maxBlastResistance)
        calcMode as Both
        return calcMode.logicalOperator.getDamageProvider(damage, predictedDamage)
    }

    abstract fun getSnapshotPos(player: Player?, ticks: Int): Vec3

    abstract class CalculationMode(
        name: String,
        private val modeValueGroup: ModeValueGroup<CalculationMode>
    ) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = modeValueGroup
    }

    class PredictOnly(
        modeValueGroup: ModeValueGroup<CalculationMode>
    ) : CalculationMode("PredictOnly", modeValueGroup)

    class Both(
        modeValueGroup: ModeValueGroup<CalculationMode>
    ) : CalculationMode("Both", modeValueGroup) {
        val logicalOperator by enumChoice("Logic", LogicalOperator.AND)
    }

    enum class LogicalOperator(override val tag: String) : Tagged {
        AND("And") {
            override fun getDamageProvider(damage: Float, damage1: Float) = AndBiDamageProvider(damage, damage1)
        },
        OR("Or") {
            override fun getDamageProvider(damage: Float, damage1: Float) = OrBiDamageProvider(damage, damage1)
        };

        abstract fun getDamageProvider(damage: Float, damage1: Float): DamageProvider

    }

}

object SelfPredict : PredictFeature("Self") {
    override fun getSnapshotPos(player: Player?, ticks: Int): Vec3 {
        return PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(ticks).pos
    }
}

object TargetPredict : PredictFeature("Target") {
    override fun getSnapshotPos(player: Player?, ticks: Int): Vec3 {
        return PlayerSimulationCache.getSimulationForOtherPlayers(player!!).getSnapshotAt(ticks).pos
    }
}
