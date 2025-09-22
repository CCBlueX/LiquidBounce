package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isHoe
import net.ccbluex.liquidbounce.utils.item.isPickaxe
import net.ccbluex.liquidbounce.utils.item.isShovel
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.block.Blocks
import net.minecraft.item.*

object MurderMysterySwordDetection {
    private val KNOWN_SWORD_ITEMS =
        hashSetOf(
            Items.GOLDEN_CARROT,
            Items.CARROT,
            Items.CARROT_ON_A_STICK,
            Items.BONE,
            Items.TROPICAL_FISH,
            Items.PUFFERFISH,
            Items.SALMON,
            Items.BLAZE_ROD,
            Items.PUMPKIN_PIE,
            Items.NAME_TAG,
            Items.APPLE,
            Items.FEATHER,
            Items.COOKIE,
            Items.SHEARS,
            Items.COOKED_SALMON,
            Items.STICK,
            Items.QUARTZ,
            Items.ROSE_BUSH,
            Items.ICE,
            Items.COOKED_BEEF,
            Items.NETHER_BRICK,
            Items.COOKED_CHICKEN,
            Items.MUSIC_DISC_BLOCKS,
            Items.MUSIC_DISC_11,
            Items.MUSIC_DISC_13,
            Items.MUSIC_DISC_CAT,
            Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR,
            Items.MUSIC_DISC_MALL,
            Items.MUSIC_DISC_MELLOHI,
            Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD,
            Items.MUSIC_DISC_WARD,
            Items.MUSIC_DISC_WAIT,
            Items.RED_DYE,
            Items.OAK_BOAT,
            Items.BOOK,
            Items.GLISTERING_MELON_SLICE,
            Items.JUNGLE_SAPLING,
            Items.PRISMARINE_SHARD,
            Items.CHARCOAL,
            Items.SUGAR_CANE,
            Items.FLINT,
            Items.BREAD,
            Items.LAPIS_LAZULI,
            Items.LEATHER,
        )
    private val KNOWN_NON_SWORD_ITEMS =
        hashSetOf(
            Items.WOODEN_SHOVEL,
            Items.GOLDEN_SHOVEL,
        )
    private val KNOWN_SWORD_BLOCKS =
        hashSetOf(
            Blocks.SPONGE,
            Blocks.DEAD_BUSH,
            Blocks.REDSTONE_TORCH,
            Blocks.CHORUS_PLANT,
        )

    fun isSword(itemStack: ItemStack): Boolean {
        val item = itemStack.item
        return when {
            item in KNOWN_NON_SWORD_ITEMS -> false
            item in KNOWN_SWORD_ITEMS -> true
            itemStack.isSword -> true
            itemStack.isPickaxe -> true
            itemStack.isShovel -> true
            itemStack.isAxe -> true
            itemStack.isHoe -> true
            item is BoatItem -> true
            item is BlockItem -> this.KNOWN_SWORD_BLOCKS.contains(item.block)
            else -> false
        }
    }
}
