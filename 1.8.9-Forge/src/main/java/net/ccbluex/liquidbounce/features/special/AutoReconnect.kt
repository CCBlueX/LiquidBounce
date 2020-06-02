/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.features.special

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
object AutoReconnect {
    const val MAX = 60000
    const val MIN = 1000

    var isEnabled = true
        private set
    var delay = 5000
        set(value) {
            isEnabled = delay < MAX

            field = value
        }
}