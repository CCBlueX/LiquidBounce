package co.uk.hexeption.utils

import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.IExtractedFunctions
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.ccbluex.liquidbounce.api.minecraft.client.shader.IFramebuffer
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.EXTFramebufferObject
import org.lwjgl.opengl.EXTPackedDepthStencil
import org.lwjgl.opengl.GL11

/**
 * Outline ESP
 *
 * @author Hexeption
 */
object OutlineUtils
{
	@JvmStatic
	fun renderOne(lineWidth: Float)
	{
		checkSetupFBO()
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
		GL11.glDisable(GL11.GL_ALPHA_TEST)
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_LIGHTING)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glLineWidth(lineWidth)
		GL11.glEnable(GL11.GL_LINE_SMOOTH)
		GL11.glEnable(GL11.GL_STENCIL_TEST)
		GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)
		GL11.glClearStencil(0xF)
		GL11.glStencilFunc(GL11.GL_NEVER, 1, 0xF)
		GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE)
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
	}

	@JvmStatic
	fun renderTwo()
	{
		GL11.glStencilFunc(GL11.GL_NEVER, 0, 0xF)
		GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE)
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
	}

	@JvmStatic
	fun renderThree()
	{
		GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xF)
		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
	}

	@JvmStatic
	fun renderFour(color: Int)
	{
		val functions: IExtractedFunctions = wrapper.functions

		RenderUtils.glColor(color)
		GL11.glDepthMask(false)
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE)
		GL11.glPolygonOffset(1.0f, -2000000.0f)

		functions.setLightmapTextureCoords(functions.getLightMapTexUnit(), 240.0f, 240.0f)
	}

	@JvmStatic
	fun renderFive()
	{
		GL11.glPolygonOffset(1.0f, 2000000.0f)
		GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_STENCIL_TEST)
		GL11.glDisable(GL11.GL_LINE_SMOOTH)
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glEnable(GL11.GL_LIGHTING)
		GL11.glEnable(GL11.GL_TEXTURE_2D)
		GL11.glEnable(GL11.GL_ALPHA_TEST)
		GL11.glPopAttrib()
	}

	@JvmStatic
	fun checkSetupFBO()
	{
		val mc: IMinecraft = wrapper.minecraft

		// Gets the FBO of Minecraft
		val fbo = mc.framebuffer

		// Check if FBO isn't null
		// Checks if screen has been resized or new FBO has been created
		if (fbo != null) if (fbo.depthBuffer > -1)
		{
			// Sets up the FBO with depth and stencil extensions (24/8 bit)
			setupFBO(mc, fbo)
			// Reset the ID to prevent multiple FBO's
			fbo.depthBuffer = -1
		}
	}

	/**
	 * Sets up the FBO with depth and stencil
	 *
	 * @param fbo
	 * Framebuffer
	 */
	@JvmStatic
	private fun setupFBO(mc: IMinecraft, fbo: IFramebuffer)
	{
		// Deletes old render buffer extensions such as depth
		// Args: Render Buffer ID
		EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer)

		// Generates a new render buffer ID for the depth and stencil extension
		val stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT()

		// Binds new render buffer by ID
		// Args: Target (GL_RENDERBUFFER_EXT), ID
		EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)

		// Adds the depth and stencil extension
		// Args: Target (GL_RENDERBUFFER_EXT), Extension (GL_DEPTH_STENCIL_EXT),
		// Width, Height
		EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, mc.displayWidth, mc.displayHeight)

		// Adds the stencil attachment
		// Args: Target (GL_FRAMEBUFFER_EXT), Attachment
		// (GL_STENCIL_ATTACHMENT_EXT), Target (GL_RENDERBUFFER_EXT), ID
		EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)

		// Adds the depth attachment
		// Args: Target (GL_FRAMEBUFFER_EXT), Attachment
		// (GL_DEPTH_ATTACHMENT_EXT), Target (GL_RENDERBUFFER_EXT), ID
		EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
	}
}
