package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PotionItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PrimitiveItemFacet
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.item.isFood
import net.ccbluex.liquidbounce.utils.item.isMiningTool
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.PotionItem
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.WindChargeItem
import net.minecraft.world.level.material.LavaFluid
import net.minecraft.world.level.material.WaterFluid

object ItemCategoryProvider {
    fun getCategory(stack: ItemStack): Int {
        return when (val item = stack.item) {
            is BowItem -> 1
            is CrossbowItem -> 2
            is FishingRodItem -> 3
            is ShieldItem -> 4
            is BlockItem -> 4
            is BucketItem -> {
                when (item.content) {
                    is WaterFluid -> 5
                    is LavaFluid -> 6
                    else -> 0
                }
            }
            is PotionItem -> {
                val areAllEffectsGood = stack.getPotionEffects()
                    .all { it.effect in PotionItemFacet.GOOD_STATUS_EFFECTS }

                if (areAllEffectsGood) 7 else 8
            }
            is EnderpearlItem -> 9
            is EggItem, is SnowballItem, is WindChargeItem -> 10
            else -> when {
                stack.isFood -> 11
                stack.isSword -> 12
                stack.isMiningTool -> 13
                else -> 0
            }
        }
    }
}
