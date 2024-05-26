package net.ccbluex.liquidbounce.utils.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.ItemStack

fun ItemStack.getPotionEffects(): Iterable<StatusEffectInstance> {
    return this.get(DataComponentTypes.POTION_CONTENTS)?.effects ?: emptyArray<StatusEffectInstance>().asIterable()
}
