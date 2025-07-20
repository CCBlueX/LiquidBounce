package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

@Suppress("unused")
internal enum class TargetRotatePosition(
    override val choiceName: String,
    val position: (LivingEntity) -> Vec3d
) : NamedChoice {
    EYES("Eyes", { target ->
        target.eyePos
    }),
    CENTER("Center", { target ->
        target.pos.add(0.0, target.height / 2.0, 0.0)
    })
}
