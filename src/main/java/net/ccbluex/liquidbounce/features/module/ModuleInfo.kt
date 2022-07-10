/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ModuleInfo(val name: String, val description: String, val category: ModuleCategory, val defaultKeyBinds: IntArray = [], val canEnable: Boolean = true, val array: Boolean = true)
