/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.render

import java.awt.image.BufferedImage

interface WIImageBuffer {
    fun parseUserSkin(image: BufferedImage?): BufferedImage?
    fun skinAvailable()
}