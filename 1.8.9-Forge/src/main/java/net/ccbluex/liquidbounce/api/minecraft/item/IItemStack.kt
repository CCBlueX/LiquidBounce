/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.item

import com.google.common.collect.Multimap
import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList

interface IItemStack {
    val maxItemUseDuration: Int
    val attributeModifiers: Multimap<String, IAttributeModifier>
    val enchantmentTagList: INBTTagList?
    var tagCompound: INBTTagCompound?
    val stackSize: Int
    val itemDamage: Int
    val item: IItem
    val itemDelay: Long
}