package net.ccbluex.liquidbounce.features.module

import org.lwjgl.input.Keyboard

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ModuleInfo(val name: String, val description: String, val category: ModuleCategory,
                            val keyBind: Int = Keyboard.CHAR_NONE, val canEnable: Boolean = true)
