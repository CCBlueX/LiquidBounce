package net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.mode

import net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.ModuleSmoothCamera
import net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.SmoothCameraMode
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.Vec3d
import kotlin.math.exp
import kotlin.math.sqrt

object SmoothCameraMotionMode : SmoothCameraMode("Motion") {
    val motion by float("Motion", 2.0f, 2.0f..10.0f)
    private val distance by float("Distance",4.0f,1.0f..10f)
    override fun cameraUpdate(
        yaw: Float,
        pitch: Float,
        pos: Vec3d,
        factor: Float,
        motion: Float,
        setSmoothPos: (Vec3d) -> Unit,
        setSmoothYaw: (Float) -> Unit,
        setSmoothPitch: (Float) -> Unit,
        setCameraPos: (Vec3d?) -> Unit
    ) {
        val player = mc.player ?: return
        val currentPos = ModuleSmoothCamera.getCameraPosition() ?: pos
        val eyePos = Vec3d(player.x, player.y + player.getEyeHeight(player.pose), player.z)
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())

        val dir = Vec3d(
            -kotlin.math.sin(yawRad) * kotlin.math.cos(pitchRad),
            -kotlin.math.sin(pitchRad),
            kotlin.math.cos(yawRad) * kotlin.math.cos(pitchRad)
        )

        val distanceSigned = if (mc.options.perspective == Perspective.THIRD_PERSON_FRONT) -distance else distance
        val targetPos = eyePos.subtract(dir.multiply(distanceSigned.toDouble()))

        val dx = targetPos.x - currentPos.x
        val dy = targetPos.y - currentPos.y
        val dz = targetPos.z - currentPos.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        val smoothFactor = factor.toDouble()
        val dynamicFactor = smoothFactor * (1.0 - exp(-distance / motion.toDouble()))

        val newCameraPos = Vec3d(
            currentPos.x + dx * dynamicFactor,
            currentPos.y + dy * dynamicFactor,
            currentPos.z + dz * dynamicFactor
        )

        setCameraPos(newCameraPos)
        setSmoothPos(newCameraPos)

    }
}

