package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.minecraft.entity.ai.attributes.AttributeModifier

class AttributeModifierImpl(val wrapped: AttributeModifier) : IAttributeModifier {
    override val amount: Double
        get() = wrapped.amount
}

inline fun IAttributeModifier.unwrap(): AttributeModifier = (this as AttributeModifierImpl).wrapped
inline fun AttributeModifier.wrap(): IAttributeModifier = AttributeModifierImpl(this)