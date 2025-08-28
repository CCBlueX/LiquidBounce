package net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.mode.SmoothCameraMotionMode
import net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.mode.SmoothCameraLerpMode
import net.ccbluex.liquidbounce.features.module.modules.render.smoothcamera.mode.SmoothCameraMotionMode.motion
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.Vec3d

/**
 * SmoothCamera module
 *
 * Makes your camera move smoother.
 */
object ModuleSmoothCamera : ClientModule("SmoothCamera", Category.RENDER, aliases = arrayOf("MotionCamera")) {
    private  val factor by float("Factor", 0.2f, 0.0f..1.0f)
    val modes = choices("Mode", SmoothCameraLerpMode, arrayOf(SmoothCameraLerpMode, SmoothCameraMotionMode))

    var smoothPos: Vec3d = Vec3d.ZERO
        private set
    var smoothYaw = 0f
        private set
    var smoothPitch = 0f
        private set
    private var cameraPos: Vec3d? = null

    override fun onEnabled() {
        super.onEnabled()
        initializeCameraPos()
    }

    override fun onDisabled() {
        smoothPos = Vec3d.ZERO
        smoothYaw = 0f
        smoothPitch = 0f
        cameraPos = null
    }

    private fun initializeCameraPos() {
        val player = mc.player ?: return
        cameraPos = player.pos
        smoothPos = player.pos
    }
    private fun isFirstPerson():
        Boolean = mc.options.perspective == Perspective.FIRST_PERSON
    @JvmStatic
    fun getCameraPosition(): Vec3d? {
        if (cameraPos == null) {
            initializeCameraPos()
        }
        return cameraPos
    }



    @JvmStatic
    fun cameraUpdate(yaw: Float, pitch: Float, pos: Vec3d) {
        if (!running) return
        modes.activeChoice.cameraUpdate(
            yaw, pitch, pos, factor, motion,
            { v: Vec3d -> smoothPos = v },
            { v: Float -> smoothYaw = v },
            { v: Float -> smoothPitch = v },
            { v: Vec3d? -> cameraPos = v }
        )
    }

    @JvmStatic
    fun shouldApplyChanges(): Boolean =
        running && !(modes.activeChoice is SmoothCameraMotionMode && isFirstPerson())
}
