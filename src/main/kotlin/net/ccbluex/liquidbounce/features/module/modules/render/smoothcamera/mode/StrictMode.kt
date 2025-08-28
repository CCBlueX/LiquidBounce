package net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.mode

import net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.ModuleSmoothCamera
import net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.SmoothCameraMode
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

object StrictMode : SmoothCameraMode("Strict") {
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
        var smoothPosLocal = ModuleSmoothCamera.smoothPos
        var smoothYawLocal = ModuleSmoothCamera.smoothYaw
        var smoothPitchLocal = ModuleSmoothCamera.smoothPitch

        if (smoothPosLocal.isLikelyZero) {
            smoothPosLocal = pos
            smoothYawLocal = yaw
            smoothPitchLocal = pitch
        }

        val eased = factor
        smoothPosLocal = smoothPosLocal.lerp(pos, eased.toDouble())
        smoothYawLocal += MathHelper.wrapDegrees(yaw - smoothYawLocal) * eased
        smoothPitchLocal += (pitch - smoothPitchLocal) * eased

        setSmoothPos(smoothPosLocal)
        setSmoothYaw(smoothYawLocal)
        setSmoothPitch(smoothPitchLocal)
        setCameraPos(smoothPosLocal)
    }
}
