/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader

import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import org.apache.commons.io.IOUtils
import org.lwjgl.opengl.*
import java.io.IOException
import java.nio.charset.StandardCharsets

abstract class Shader(fragmentShader: String) : MinecraftInstance()
{
	var programId = 0
	private var uniformsMap: MutableMap<String, Int>? = null

	open fun startShader()
	{
		GL11.glPushMatrix()
		GL20.glUseProgram(programId)

		if (uniformsMap == null)
		{
			uniformsMap = hashMapOf()
			setupUniforms()
		}

		updateUniforms()
	}

	open fun stopShader()
	{
		GL20.glUseProgram(0)
		GL11.glPopMatrix()
	}

	abstract fun setupUniforms()
	abstract fun updateUniforms()

	private fun setUniform(uniformName: String, location: Int)
	{
		uniformsMap?.set(uniformName, location)
	}

	fun setupUniform(uniformName: String)
	{
		setUniform(uniformName, GL20.glGetUniformLocation(programId, uniformName))
	}

	fun getUniform(uniformName: String): Int = uniformsMap?.get(uniformName) ?: -1

	companion object
	{
		private fun createShader(shaderSource: CharSequence, shaderType: Int): Int
		{
			var shader = 0

			return try
			{
				shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType)

				if (shader == 0) return 0

				ARBShaderObjects.glShaderSourceARB(shader, shaderSource)
				ARBShaderObjects.glCompileShaderARB(shader)

				if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) throw RuntimeException("Error creating shader: " + getLogInfo(shader))

				shader
			}
			catch (e: Exception)
			{
				ARBShaderObjects.glDeleteObjectARB(shader)
				throw e
			}
		}

		private fun getLogInfo(i: Int): String = ARBShaderObjects.glGetInfoLogARB(i, ARBShaderObjects.glGetObjectParameteriARB(i, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB))
	}

	init
	{
		try
		{
			val vertexStream = javaClass.getResourceAsStream("/assets/minecraft/liquidbounce/shader/vertex.vert")
			val vertexShaderID = createShader(IOUtils.toString(vertexStream, StandardCharsets.UTF_8), ARBVertexShader.GL_VERTEX_SHADER_ARB)

			IOUtils.closeQuietly(vertexStream)

			val fragmentStream = javaClass.getResourceAsStream("/assets/minecraft/liquidbounce/shader/fragment/$fragmentShader")
			val fragmentShaderID = createShader(IOUtils.toString(fragmentStream, StandardCharsets.UTF_8), ARBFragmentShader.GL_FRAGMENT_SHADER_ARB)

			IOUtils.closeQuietly(fragmentStream)

			// Workaround for return is not allowed in init block
			run {
				if (vertexShaderID == 0 || fragmentShaderID == 0) return@run

				programId = ARBShaderObjects.glCreateProgramObjectARB()

				if (programId == 0) return@run

				ARBShaderObjects.glAttachObjectARB(programId, vertexShaderID)
				ARBShaderObjects.glAttachObjectARB(programId, fragmentShaderID)
				ARBShaderObjects.glLinkProgramARB(programId)
				ARBShaderObjects.glValidateProgramARB(programId)

				logger.info("[Shader] Successfully loaded: {}", fragmentShader)
			}
		}
		catch (e: IOException)
		{
			logger.error("Can't load shaders from assets", e)
		}
	}
}
