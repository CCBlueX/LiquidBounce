package net.ccbluex.liquidbounce.api.minecraft.client.shader

interface IFramebuffer
{
	val framebufferTexture: Int
	var depthBuffer: Int

	fun bindFramebuffer(b: Boolean)
	fun framebufferClear()
	fun deleteFramebuffer()
}
