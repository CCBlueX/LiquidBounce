package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

@Suppress("MaxLineLength", "MagicNumber")
internal object TargetEntityMovementPrediction : ToggleableConfigurable(ElytraRotationProcessor, "Prediction", true) {
    private val mode by enumChoice("Mode", PredictMode.SIMPLE)
    private val glidingOnly by boolean("GlidingOnly", true)
    private val multiplier by floatRange("Multiplier", 1.8f..2f, 0.5f..3f)

    internal fun predictPosition(
        target: LivingEntity,
        targetPosition: Vec3d
    ) = when {
        !enabled || (glidingOnly && !target.isGliding) -> targetPosition
        else -> mode.predict(target, targetPosition, multiplier.random().toDouble())
    }
}

@Suppress("unused", "MagicNumber")
private enum class PredictMode(
    override val choiceName: String,
    val predict: (target: LivingEntity, targetPosition: Vec3d, multiplier: Double) -> Vec3d
) : NamedChoice {
    SIMPLE("Simple", { target, targetPosition, multiplier ->
        targetPosition + target.velocity * multiplier
    }),
    WITH_GRAVITY("WithGravity", { target, targetPosition, multiplier ->
        SIMPLE.predict(
            target,
            targetPosition,
            multiplier
        ) - Vec3d(
            0.0,
            0.5 * 0.05 * multiplier * multiplier,
            0.0
        )
    })
}
