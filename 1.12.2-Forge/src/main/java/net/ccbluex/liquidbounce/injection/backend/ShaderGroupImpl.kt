/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.shader.IShaderGroup
import net.minecraft.client.shader.ShaderGroup

class ShaderGroupImpl(val wrapped: ShaderGroup) : IShaderGroup {
    override val shaderGroupName: String
        get() = wrapped.shaderGroupName


    override fun equals(other: Any?): Boolean {
        return other is ShaderGroupImpl && other.wrapped == this.wrapped
    }
}

inline fun IShaderGroup.unwrap(): ShaderGroup = (this as ShaderGroupImpl).wrapped
inline fun ShaderGroup.wrap(): IShaderGroup = ShaderGroupImpl(this)