package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.sqrt

object ModuleKeepRange : ClientModule("KeepRange", Category.COMBAT) {

    private val distanceRange by floatRange("Distance", 3f..4.0f, 0.0f..10f)
    private val onlyForward by boolean("OnlyForward", true)
    private val predict by boolean("Predictive", true)
    private val keepTick by int("KeepTick", 10, 0..40)
    private val restTick by int("RestTick", 4, 0..40)

    private var ticksElapsed = 0

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent> { event ->

        val currentTarget = (ModuleKillAura.targetTracker.target)
            ?.takeIf { it is PlayerEntity } as? PlayerEntity ?: return@handler

        ticksElapsed++
        if (ticksElapsed >= keepTick + restTick) ticksElapsed = 0

        var distance = mc.player?.distanceTo(currentTarget) ?: return@handler
        if (predict) distance = getPredictedDistance(currentTarget)


        if (!currentTarget.isDead && !(distance >= distanceRange.endInclusive)){
            if (distance <= distanceRange.start && ticksElapsed < keepTick) {
                event.directionalInput = if (onlyForward) {
                    event.directionalInput.copy(forwards = false)
                } else {
                    event.directionalInput.copy(
                        forwards = false,
                        left = false,
                        right = false,
                    )
                }
            }
        }
    }
    private fun getFinalHorizontalPosition(tgt: PlayerEntity): Vec3d {
        var mx = tgt.velocity.x
        var mz = tgt.velocity.z
        var fx = tgt.x
        var fz = tgt.z

        while (abs(mx) > 5.0E-4 && abs(mz) > 5.0E-4) {
            fx += mx
            fz += mz
            mx *= 0.98
            mz *= 0.98
        }

        return Vec3d(fx, -999.0, fz)
    }

    private fun getPredictedDistance(tgt: PlayerEntity): Float {
        val playerPos = mc.player!!.pos
        val predicted = getFinalHorizontalPosition(tgt)
        val dx = playerPos.x - predicted.x
        val dz = playerPos.z - predicted.z
        return sqrt(dx * dx + dz * dz).toFloat()
    }

    override fun onDisabled() {
        ticksElapsed = 0
    }
}
