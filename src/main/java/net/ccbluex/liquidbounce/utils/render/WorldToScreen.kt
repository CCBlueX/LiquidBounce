package net.ccbluex.liquidbounce.utils.render

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import org.lwjgl.util.vector.Vector4f

object WorldToScreen
{
	fun getMatrix(matrix: Int): Matrix4f
	{
		val floatBuffer = BufferUtils.createFloatBuffer(16)
		GL11.glGetFloat(matrix, floatBuffer)
		return Matrix4f().load(floatBuffer) as Matrix4f
	}

	//	public static Vector2f worldToScreen(final Vector3f pointInWorld, final int screenWidth, final int screenHeight)
	//	{
	//		return worldToScreen(pointInWorld, getMatrix(GL11.GL_MODELVIEW_MATRIX), getMatrix(GL11.GL_PROJECTION_MATRIX), screenWidth, screenHeight);
	//	}

	fun worldToScreen(pointInWorld: Vector3f, view: Matrix4f, projection: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2f?
	{
		val clipSpacePos = Vector4f(pointInWorld.x, pointInWorld.y, pointInWorld.z, 1.0f) * view * projection
		val ndcSpacePos = Vector3f(clipSpacePos.x / clipSpacePos.w, clipSpacePos.y / clipSpacePos.w, clipSpacePos.z / clipSpacePos.w)

//        System.out.println(pointInNdc);
		val screenX = (ndcSpacePos.x + 1.0f) * 0.5f * screenWidth
		val screenY = (1.0f - ndcSpacePos.y) * 0.5f * screenHeight

		// nPlane = -1, fPlane = 1
		return if (ndcSpacePos.z < -1.0 || ndcSpacePos.z > 1.0) null else Vector2f(screenX, screenY)
	}

	private operator fun Vector4f.times(mat: Matrix4f): Vector4f = Vector4f(x * mat.m00 + y * mat.m10 + z * mat.m20 + w * mat.m30, x * mat.m01 + y * mat.m11 + z * mat.m21 + w * mat.m31, x * mat.m02 + y * mat.m12 + z * mat.m22 + w * mat.m32, x * mat.m03 + y * mat.m13 + z * mat.m23 + w * mat.m33)
}
