/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glGetFloat
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import org.lwjgl.util.vector.Vector4f
import kotlin.math.abs

object WorldToScreen : MinecraftInstance() {
    fun getMatrix(matrix: Int): Matrix4f {
        val floatBuffer = BufferUtils.createFloatBuffer(16)
        
        glGetFloat(matrix, floatBuffer)
        
        return Matrix4f().load(floatBuffer) as Matrix4f
    }

    fun worldToScreen(
        pointInWorld: Vector3f,
        viewMatrix: Matrix4f = getMatrix(GL11.GL_MODELVIEW_MATRIX),
        projectionMatrix: Matrix4f = getMatrix(GL11.GL_PROJECTION_MATRIX),
        screenWidth: Int = mc.displayWidth,
        screenHeight: Int = mc.displayHeight
    ): Vector2f? {
        val clipSpacePos = Vector4f(pointInWorld.x, pointInWorld.y, pointInWorld.z, 1f) * viewMatrix * projectionMatrix

        val ndcSpacePos = Vector3f(clipSpacePos.x, clipSpacePos.y, clipSpacePos.z).scale(1 / clipSpacePos.w) as Vector3f

        val screenX = (ndcSpacePos.x + 1f) / 2f * screenWidth
        val screenY = (1f - ndcSpacePos.y) / 2f * screenHeight

        return if (abs(ndcSpacePos.z) > 1) null
        else Vector2f(screenX, screenY)
    }
}

private operator fun Vector4f.times(mat: Matrix4f) = Vector4f(
    this.x * mat.m00 + this.y * mat.m10 + this.z * mat.m20 + this.w * mat.m30,
    this.x * mat.m01 + this.y * mat.m11 + this.z * mat.m21 + this.w * mat.m31,
    this.x * mat.m02 + this.y * mat.m12 + this.z * mat.m22 + this.w * mat.m32,
    this.x * mat.m03 + this.y * mat.m13 + this.z * mat.m23 + this.w * mat.m33
)
