@file:Suppress("LongParameterList")
package net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.minecraft.util.math.Vec3d

abstract class SmoothCameraMode(name: String) : Choice(name) {
    override val parent: ChoiceConfigurable<SmoothCameraMode>
        get() = ModuleSmoothCamera.modes

    abstract fun cameraUpdate(
        yaw: Float,
        pitch: Float,
        pos: Vec3d,
        factor: Float,
        motion: Float,
        setSmoothPos: (Vec3d) -> Unit,
        setSmoothYaw: (Float) -> Unit,
        setSmoothPitch: (Float) -> Unit,
        setCameraPos: (Vec3d?) -> Unit
    )
}
