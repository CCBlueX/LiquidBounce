package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IGlStateManager
import net.minecraft.client.renderer.GlStateManager

object GlStateManagerImpl : IGlStateManager {
    override fun bindTexture(textureID: Int) = GlStateManager.bindTexture(textureID)

}