package net.ccbluex.liquidbounce.api

annotation class SupportsMinecraftVersions(vararg val value: MinecraftVersion)

enum class MinecraftVersion
{
	MC_1_8, MC_1_12
}
