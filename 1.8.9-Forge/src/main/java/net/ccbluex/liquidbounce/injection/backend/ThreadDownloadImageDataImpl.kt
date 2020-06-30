/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.render.IThreadDownloadImageData
import net.minecraft.client.renderer.ThreadDownloadImageData

class ThreadDownloadImageDataImpl(val wrapped: ThreadDownloadImageData) : IThreadDownloadImageData