package net.ccbluex.liquidbounce.injection.backend

object Backend {
    const val MINECRAFT_VERSION = "1.12.2"
    const val MINECRAFT_VERSION_MAJOR = 1
    const val MINECRAFT_VERSION_IMMEDIATE = 12
    const val MINECRAFT_VERSION_MINOR = 2


    public inline fun BACKEND_UNSUPPORTED(): Nothing = throw NotImplementedError("$MINECRAFT_VERSION doesn't support this feature'")
}