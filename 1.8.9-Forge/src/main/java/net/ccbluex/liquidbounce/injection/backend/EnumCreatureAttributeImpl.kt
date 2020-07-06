/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.entity.IEnumCreatureAttribute
import net.minecraft.entity.EnumCreatureAttribute

class EnumCreatureAttributeImpl(val wrapped: EnumCreatureAttribute) : IEnumCreatureAttribute {

    override fun equals(other: Any?): Boolean {
        return other is EnumCreatureAttributeImpl && other.wrapped == this.wrapped
    }
}

inline fun IEnumCreatureAttribute.unwrap(): EnumCreatureAttribute = (this as EnumCreatureAttributeImpl).wrapped
inline fun EnumCreatureAttribute.wrap(): IEnumCreatureAttribute = EnumCreatureAttributeImpl(this)