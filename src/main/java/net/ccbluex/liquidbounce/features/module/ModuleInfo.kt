/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import org.lwjgl.input.Keyboard

@Retention(AnnotationRetention.RUNTIME)
annotation class ModuleInfo(val name: String, val description: String, val category: ModuleCategory,
                            val keyBind: Int = Keyboard.CHAR_NONE, val canEnable: Boolean = true, val array: Boolean = true)
