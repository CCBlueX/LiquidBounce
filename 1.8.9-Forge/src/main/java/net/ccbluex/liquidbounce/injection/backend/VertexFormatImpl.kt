/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.render.vertex.IVertexFormat
import net.minecraft.client.renderer.vertex.VertexFormat

class VertexFormatImpl(val wrapped: VertexFormat) : IVertexFormat {
    override fun equals(other: Any?): Boolean {
        return other is VertexFormatImpl && other.wrapped == this.wrapped
    }
}

inline fun IVertexFormat.unwrap(): VertexFormat = (this as VertexFormatImpl).wrapped
inline fun VertexFormat.wrap(): IVertexFormat = VertexFormatImpl(this)