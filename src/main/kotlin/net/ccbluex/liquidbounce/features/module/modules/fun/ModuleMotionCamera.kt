package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.client.option.Perspective
import net.minecraft.util.math.Vec3d
import kotlin.math.exp
object ModuleMotionCamera : ClientModule("MotionCamera", Category.FUN, aliases = arrayOf("ActionCamera")) {

    private val smoothness by float("Smoothness", 0.3f, 0.1f..0.95f)
    private val maxDistance by float("MaxDistance", 15.0f, 1.0f..50.0f)

    private var cameraPos: Vec3d? = null
    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler run { cameraPos = null }
        val eyePos = Vec3d(player.x, player.y + player.getEyeHeight(player.pose), player.z)
        updateCameraPos(eyePos)
    }
    @Suppress("unused")
    private val worldChange = handler<WorldChangeEvent> { event ->
        initializeCameraPos()
    }

    override fun onEnabled() {
        super.onEnabled()
        initializeCameraPos()
    }

    override fun onDisabled() {
        super.onDisabled()
        cameraPos = null
    }

    private fun initializeCameraPos() {
        val player = mc.player ?: return
        cameraPos = player.pos
    }

    private fun isFirstPerson(): Boolean {
        return mc.options.perspective == Perspective.FIRST_PERSON
    }

    fun getCameraPosition(): Vec3d? {
        if (cameraPos == null) {
            initializeCameraPos()
        }
        if (isFirstPerson() && mc.player != null) {
            val player = mc.player!!
            return Vec3d(player.x, player.y + player.getEyeHeight(player.pose), player.z)
        }
        return cameraPos
    }

    fun updateCameraPos(playerPos: Vec3d) {
        val player = mc.player ?: return
        if (cameraPos == null) {
            cameraPos = playerPos
            return
        }

        val currentPos = cameraPos!!
        val distance = currentPos.distanceTo(playerPos)
        val maxDist = maxDistance.toDouble()
        if (distance > maxDist) {
            cameraPos = playerPos
            return
        }

        val smoothFactor = smoothness.toDouble()
        val dynamicFactor = smoothFactor * (1.0 - exp(-distance / maxDist))
        val dx = playerPos.x - currentPos.x
        val dy = playerPos.y + player.getEyeHeight(player.pose) - currentPos.y
        val dz = playerPos.z - currentPos.z
        cameraPos = Vec3d(
            currentPos.x + dx * dynamicFactor,
            currentPos.y + dy * dynamicFactor,
            currentPos.z + dz * dynamicFactor
        )
    }
    @JvmStatic
    fun shouldModifyCamera(): Boolean {
        return enabled &&  !isFirstPerson()
    }
}
