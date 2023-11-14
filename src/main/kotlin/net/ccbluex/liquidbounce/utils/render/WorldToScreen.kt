package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Vector3f

object WorldToScreen {
    fun calculateScreenPos(
        pos: Vec3d,
        mvMatrix: Matrix4f,
        projectionMatrix: Matrix4f,
        cameraPos: Vec3d = mc.gameRenderer.camera.pos,
    ): Vec3? {
        val relativePos = pos - cameraPos

        val transformedPos = Matrix4f(projectionMatrix).mul(mvMatrix).transformProject(
            relativePos.x.toFloat(),
            relativePos.y.toFloat(),
            relativePos.z.toFloat(),
            Vector3f()
        )

        val screenPos = transformedPos.mul(1.0F, -1.0F, 1.0F).add(1.0F, 1.0F, 0.0F)
            .mul(0.25F * mc.framebuffer.viewportWidth, 0.25F * mc.framebuffer.viewportHeight, 1.0F)

        return if (transformedPos.z < 1.0F) Vec3(screenPos.x, screenPos.y, transformedPos.z) else null
    }

}
