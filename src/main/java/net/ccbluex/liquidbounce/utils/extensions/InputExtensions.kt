/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.util.MovementInput

fun MovementInput.reset() {
    this.moveForward = 0f
    this.moveStrafe = 0f
    this.jump = false
    this.sneak = false
}