/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

// In order to get the annotation with reflections successfully, we don't want to target the getter method but the field.
@Target(AnnotationTarget.FIELD)
annotation class FontDetails(val fontName: String, val fontSize: Int = -1)