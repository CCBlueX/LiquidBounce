package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.MinecraftVersion

object Backend {
    public const val MINECRAFT_VERSION = "1.12.2"
    public const val MINECRAFT_VERSION_MAJOR = 1
    public const val MINECRAFT_VERSION_MINOR = 12
    public const val MINECRAFT_VERSION_PATCH = 2
    public val REPRESENTED_BACKEND_VERSION = MinecraftVersion.MC_1_12


    public inline fun BACKEND_UNSUPPORTED(): Nothing = throw NotImplementedError("$MINECRAFT_VERSION doesn't support this feature'")
}