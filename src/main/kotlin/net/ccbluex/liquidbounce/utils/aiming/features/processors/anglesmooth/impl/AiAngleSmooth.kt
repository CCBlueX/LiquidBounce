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
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.deeplearn.combat.BundledCombatModel
import net.ccbluex.liquidbounce.deeplearn.combat.CombatController
import net.ccbluex.liquidbounce.deeplearn.combat.CombatDecision
import net.ccbluex.liquidbounce.deeplearn.combat.CombatModels
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.deeplearn.model.ModelStatus
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.DebuggedLineSegment
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.AngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.NoneAngleSmooth
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.minecraft.world.entity.LivingEntity
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Aims the way the model decides. [fallback] aims whenever it does not: without a model, while the fight
 * history fills up, and on the way back to the camera.
 */
class AiAngleSmooth(
    parent: ModeValueGroup<*>,
    private val fallback: AngleSmooth,
) : AngleSmooth("AI", parent, listOf("Minarai")) {
    @UnstableAddonApi
    val model by enumChoice("Model", BundledCombatModel.DEFAULT)

    @UnstableAddonApi
    val randomness by float("Randomness", 0.5f, 0f..1f)

    @UnstableAddonApi
    val prediction by int("Prediction", 2, 0..4, "ticks")

    private val speed by float("Speed", 1f, 0.5f..1.5f)
    private val maxTurn by float("MaxTurn", 60f, 10f..180f)
    private val assist = modes(this, "Assist") {
        arrayOf(
            NoneAngleSmooth(it),
            InterpolationAngleSmooth(it, 2..5, 2..5, 95..100),
            LinearAngleSmooth(it, horizontalTurnSpeed = 5f..5f, verticalTurnSpeed = 5f..5f),
        )
    }

    @UnstableAddonApi
    val controller = CombatController(this)

    private var lastSpeed = 1f
    private var fellBack = true
    private var notified: ModelStatus? = null

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        val target = rotationTarget.entity as? LivingEntity
        val decision = target?.let { controller.decide(it, model, randomness, prediction) }
        debug(target, currentRotation, decision)
        if (decision == null) {
            // Without a decision the history may just be filling up, which is only worth a message if it never ends
            ModelRegistry.status(CombatModels.SLOT, model.id).takeIf { it != ModelStatus.READY }?.let(::notify)
            fellBack = true
            return fallback.process(rotationTarget, currentRotation, targetRotation)
        }
        notified = null
        fellBack = false
        val yaw = (decision.yaw * speed).coerceIn(-maxTurn, maxTurn)
        val pitch = (decision.pitch * speed).coerceIn(-maxTurn, maxTurn)
        lastSpeed = max(abs(yaw), abs(pitch)).coerceAtLeast(1f)
        val rotation = Rotation(currentRotation.yaw + yaw, (currentRotation.pitch + pitch).coerceIn(-90f, 90f))
        return assist.activeMode.process(rotationTarget, rotation, targetRotation)
    }

    override fun calculateTicks(currentRotation: Rotation, targetRotation: Rotation): Int {
        if (fellBack) {
            return fallback.calculateTicks(currentRotation, targetRotation)
        }
        val delta = currentRotation.rotationDeltaTo(targetRotation)
        return ceil(max(abs(delta.deltaYaw), abs(delta.deltaPitch)) / lastSpeed).toInt().coerceAtLeast(1)
    }

    override fun prepareDeserialize(jsonObject: JsonObject) = migrateAiAngleSmooth(jsonObject)

    private fun notify(status: ModelStatus) {
        if (notified != status) {
            notified = status
            chat(markAsError(translation("liquidbounce.rotationSystem.angleSmooth.ai.notReady", status.text(),
                fallback.name)))
        }
    }

    private fun debug(target: LivingEntity?, view: Rotation, decision: CombatDecision?) {
        debugParameter("Target") {
            target?.let { "%s, %.1f blocks, hurt %d".format(it.scoreboardName, it.distanceTo(player), it.hurtTime) }
        }
        debugParameter("History") {
            controller.lastStep?.takeIf { it.tick == player.tickCount }?.let { "${it.history} ticks" }
        }
        debugParameter("Turn") {
            decision?.let { "%.1f yaw, %.1f pitch%s".format(it.yaw, it.pitch, if (it.clamped) ", clamped" else "") }
        }
        val eyes = player.eyePosition
        debugGeometry("View") { DebuggedLineSegment(eyes, eyes.add(view.directionVector.scale(4.0)), Color4b.RED) }
        debugGeometry("Decision") {
            decision?.let {
                val next = Rotation(view.yaw + it.yaw, view.pitch + it.pitch)
                DebuggedLineSegment(eyes, eyes.add(next.directionVector.scale(4.0)), Color4b.GREEN)
            }
        }
    }
}

/**
 * Before the rotation models, AI chose between the old models in a mode group and called Assist Correction. Their
 * choice becomes the default model.
 */
internal fun migrateAiAngleSmooth(ai: JsonObject) {
    val values = ai["value"]?.takeIf { it.isJsonArray }?.asJsonArray?.asList() ?: return
    fun named(name: String) = values.firstOrNull { (it as? JsonObject)?.get("name")?.asString == name } as JsonObject?

    named("Model")?.takeIf { it.has("choices") }?.let(values::remove)
    if (named("Assist") == null) {
        named("Correction")?.addProperty("name", "Assist")
    }
}
