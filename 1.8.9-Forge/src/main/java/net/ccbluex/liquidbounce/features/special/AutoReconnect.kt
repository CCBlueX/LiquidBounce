package net.ccbluex.liquidbounce.features.special

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
object AutoReconnect {
    var enabled = true
    var delay = 5000
}