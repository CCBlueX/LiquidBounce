/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.minecraft.block.material.Material

class MaterialImpl(val wrapped: Material) : IMaterial {
    override val isReplaceable: Boolean
        get() = wrapped.isReplaceable
}