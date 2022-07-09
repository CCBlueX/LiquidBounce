/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.api.MinecraftVersion

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ModuleInfo(val name: String, val description: String, val category: ModuleCategory, val defaultKeyBinds: IntArray = [], val canEnable: Boolean = true, val array: Boolean = true, val supportedVersions: Array<MinecraftVersion> = [MinecraftVersion.MC_1_8, MinecraftVersion.MC_1_12])
