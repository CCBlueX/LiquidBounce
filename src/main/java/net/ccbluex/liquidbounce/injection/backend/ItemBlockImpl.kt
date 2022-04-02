/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.item.IItemBlock
import net.minecraft.item.ItemBlock

class ItemBlockImpl(wrapped: ItemBlock) : ItemImpl<ItemBlock>(wrapped), IItemBlock {
    override val block: IBlock
        get() = BlockImpl(wrapped.block)
    override val unlocalizedName: String
        get() = wrapped.unlocalizedName
}