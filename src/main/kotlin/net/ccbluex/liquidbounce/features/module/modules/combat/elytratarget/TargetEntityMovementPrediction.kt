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

package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

@Suppress("MaxLineLength", "MagicNumber")
internal object TargetEntityMovementPrediction : ToggleableValueGroup(ElytraRotationProcessor, "Prediction", true) {
    private val mode by enumChoice("Mode", PredictMode.SIMPLE)
    private val glidingOnly by boolean("GlidingOnly", true)
    private val multiplier by floatRange("Multiplier", 1.8f..2f, 0.5f..3f)

    internal fun predictPosition(
        target: LivingEntity,
        targetPosition: Vec3
    ) = when {
        !enabled || (glidingOnly && !target.isFallFlying) -> targetPosition
        else -> mode.predict(target, targetPosition, multiplier.random().toDouble())
    }
}

@Suppress("unused", "MagicNumber")
private enum class PredictMode(
    override val tag: String,
    val predict: (target: LivingEntity, targetPosition: Vec3, multiplier: Double) -> Vec3
) : Tagged {
    SIMPLE("Simple", { target, targetPosition, multiplier ->
        targetPosition + target.deltaMovement * multiplier
    }),
    WITH_GRAVITY("WithGravity", { target, targetPosition, multiplier ->
        SIMPLE.predict(
            target,
            targetPosition,
            multiplier
        ) - Vec3(
            0.0,
            0.5 * 0.05 * multiplier * multiplier,
            0.0
        )
    })
}
