package net.ccbluex.liquidbounce.api

annotation class SupportsMinecraftVersions(val value: Array<MinecraftVersion>)

enum class MinecraftVersion {
    MC_1_8, MC_1_12
}