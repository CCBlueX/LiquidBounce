/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.IExtractedFunctions
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.entity.IEnumCreatureAttribute
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.block.Block

object ExtractedFunctionsImpl : IExtractedFunctions {
    override fun getBlockById(id: Int): IBlock? = Block.getBlockById(id)?.let(::BlockImpl)

    override fun getIdFromBlock(block: IBlock): Int = Block.getIdFromBlock(block.unwrap())

    override fun getModifierForCreature(heldItem: IItemStack?, creatureAttribute: IEnumCreatureAttribute): Float {
        TODO("Not yet implemented")
    }


}