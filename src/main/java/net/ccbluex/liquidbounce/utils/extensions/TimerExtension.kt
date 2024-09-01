/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.client.render.ClientTickTracker

/**
 * Get Game Timer
 */
var ClientTickTracker.timerSpeed: Float?
    get() = field_1039
    set(value) {
        field_1039 = value ?: 1F
    }

