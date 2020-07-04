/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.tileentity

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos

interface ITileEntity {
    val pos: WBlockPos
}