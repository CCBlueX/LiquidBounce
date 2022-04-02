/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.minecraft.util.ResourceLocation

class ResourceLocationImpl(val wrapped: ResourceLocation) : IResourceLocation {
    override val resourcePath: String
        get() = wrapped.resourcePath


    override fun equals(other: Any?): Boolean {
        return other is ResourceLocationImpl && other.wrapped == this.wrapped
    }
}

inline fun IResourceLocation.unwrap(): ResourceLocation = (this as ResourceLocationImpl).wrapped
inline fun ResourceLocation.wrap(): IResourceLocation = ResourceLocationImpl(this)