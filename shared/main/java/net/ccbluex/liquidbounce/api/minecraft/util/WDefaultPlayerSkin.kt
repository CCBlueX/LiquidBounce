package net.ccbluex.liquidbounce.api.minecraft.util

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.util.*

object WDefaultPlayerSkin : MinecraftInstance()
{
    private val TEXTURE_STEVE = classProvider.createResourceLocation("textures/entity/steve.png")
    private val TEXTURE_ALEX = classProvider.createResourceLocation("textures/entity/alex.png")

    @JvmStatic
    fun getDefaultSkinLegacy(): IResourceLocation = TEXTURE_STEVE

    @JvmStatic
    fun getDefaultSkin(playerUUID: UUID): IResourceLocation = if (isSlimSkin(playerUUID)) TEXTURE_ALEX else TEXTURE_STEVE

    @JvmStatic
    fun getSkinType(playerUUID: UUID): String = if (isSlimSkin(playerUUID)) "slim" else "default"

    @JvmStatic
    private fun isSlimSkin(playerUUID: UUID): Boolean = playerUUID.hashCode() and 1 == 1
}
