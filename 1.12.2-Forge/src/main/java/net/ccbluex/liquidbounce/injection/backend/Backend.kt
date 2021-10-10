package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.MinecraftVersion

object Backend
{
	const val MINECRAFT_VERSION = "1.12.2"
	const val MINECRAFT_VERSION_MAJOR = 1
	const val MINECRAFT_VERSION_MINOR = 12
	const val MINECRAFT_VERSION_PATCH = 2
	val REPRESENTED_BACKEND_VERSION = MinecraftVersion.MC_1_12

	fun BACKEND_UNSUPPORTED(): Nothing = throw NotImplementedError("$MINECRAFT_VERSION doesn't support this feature")
}
