package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.MinecraftVersion

object Backend
{
	const val MINECRAFT_VERSION = "1.8.9"
	const val MINECRAFT_VERSION_MAJOR = 1
	const val MINECRAFT_VERSION_MINOR = 8
	const val MINECRAFT_VERSION_PATCH = 9

	val REPRESENTED_BACKEND_VERSION = MinecraftVersion.MC_1_8

	@Suppress("FunctionName")
	fun BACKEND_UNSUPPORTED(): Nothing = throw NotImplementedError("$MINECRAFT_VERSION doesn't support this feature'")
}
