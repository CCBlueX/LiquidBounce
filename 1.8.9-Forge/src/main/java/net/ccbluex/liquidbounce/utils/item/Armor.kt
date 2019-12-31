package net.ccbluex.liquidbounce.utils.item

import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemArmor

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
object Armor {

    private val helmet = intArrayOf(
            Item.getIdFromItem(Items.diamond_helmet),
            Item.getIdFromItem(Items.iron_helmet),
            Item.getIdFromItem(Items.chainmail_helmet),
            Item.getIdFromItem(Items.golden_helmet),
            Item.getIdFromItem(Items.leather_helmet)
    )

    private val chestplate = intArrayOf(
            Item.getIdFromItem(Items.diamond_chestplate),
            Item.getIdFromItem(Items.iron_chestplate),
            Item.getIdFromItem(Items.chainmail_chestplate),
            Item.getIdFromItem(Items.golden_chestplate),
            Item.getIdFromItem(Items.leather_chestplate)
    )

    private val leggins = intArrayOf(
            Item.getIdFromItem(Items.diamond_leggings),
            Item.getIdFromItem(Items.iron_leggings),
            Item.getIdFromItem(Items.chainmail_leggings),
            Item.getIdFromItem(Items.golden_leggings),
            Item.getIdFromItem(Items.leather_leggings)
    )

    private val boots = intArrayOf(
            Item.getIdFromItem(Items.diamond_boots),
            Item.getIdFromItem(Items.iron_boots),
            Item.getIdFromItem(Items.chainmail_boots),
            Item.getIdFromItem(Items.golden_boots),
            Item.getIdFromItem(Items.leather_boots)
    )

    /**
     * Get armor array by armor [type]
     */
    @JvmStatic
    fun getArmorArray(type: Int): IntArray? {
        return when (type) {
            0 -> boots
            1 -> leggins
            2 -> chestplate
            3 -> helmet
            else -> null
        }
    }

    /**
     * Get armor array by armor type
     */
    @JvmStatic
    fun getArmorArray(itemArmor: ItemArmor): IntArray? {
        return when (itemArmor.armorType) {
            3 -> boots
            2 -> leggins
            1 -> chestplate
            0 -> helmet
            else -> null
        }
    }

    /**
     * Get armor inventory slot by [type]
     */
    @JvmStatic
    fun getArmorSlot(type: Int): Int {
        return when (type) {
            0 -> 8
            1 -> 7
            2 -> 6
            3 -> 5
            else -> -1
        }
    }
}