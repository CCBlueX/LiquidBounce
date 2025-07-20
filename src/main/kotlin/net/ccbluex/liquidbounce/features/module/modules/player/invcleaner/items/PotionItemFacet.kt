package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.kotlin.mapInt
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.Tier
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.LingeringPotionItem
import net.minecraft.item.PotionItem
import net.minecraft.item.SplashPotionItem
import java.util.*

class PotionItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    override val category: ItemCategory
        get() = ItemCategory(ItemType.POTION, 0)

    companion object {
        private val COMPARATOR = ComparatorChain(
            PreferHigherTierPotions,
            PreferAmplifier,
            PreferSplashPotions,
            PreferHigherDurationPotions,
            PREFER_ITEMS_IN_HOTBAR,
            STABILIZE_COMPARISON
        )

        /**
         * Prefers potions which have more status effects of higher Tier.
         * For example:
         * - `S > A`
         * - `A + A > A + B`
         * - `A + A + F > A + A`
         * - etc.
         */
        private object PreferHigherTierPotions : Comparator<PotionItemFacet> {
            override fun compare(o1: PotionItemFacet, o2: PotionItemFacet): Int = compareValuesBy(o1, o2) { o ->
                o.itemStack.getPotionEffects()
                    .mapTo(ObjectArrayList(8)) { it.effectType.value().tier }
                    .apply { sortDescending() }
            }
        }

        /**
         * This check is pretty random as it does not care which effect it compares.
         * - Anything (S-Tier) II + Anything (S-Tier) I > Anything (S-Tier) I + Anything (S-Tier) I
         */
        private object PreferAmplifier : Comparator<PotionItemFacet> {
            override fun compare(o1: PotionItemFacet, o2: PotionItemFacet): Int = compareValuesBy(o1, o2) { o ->
                o.itemStack.getPotionEffects()
                    .sortedByDescending { it.effectType.value().tier }
                    .mapInt { it.amplifier }
            }
        }

        /**
         * Prefers quick and targeted potions: `splash potion > drinkable potion > lingering potion`
         */
        private object PreferSplashPotions : Comparator<PotionItemFacet> {
            override fun compare(o1: PotionItemFacet, o2: PotionItemFacet): Int {
                val tier1 = tierOfPotionType(o1.itemStack.item as PotionItem)
                val tier2 = tierOfPotionType(o2.itemStack.item as PotionItem)

                return tier1.compareTo(tier2)
            }

            fun tierOfPotionType(potionItem: PotionItem): Tier {
                return when (potionItem) {
                    is SplashPotionItem -> Tier.S
                    is LingeringPotionItem -> Tier.B
                    else -> Tier.A
                }
            }
        }

        /**
         * Prefers higher duration of higher tiers.
         * - `S (1:00) > S (0:30)`
         * - `S (0:30) + A (1:00) > S (1:00) + A (20:00)`
         */
        private object PreferHigherDurationPotions : Comparator<PotionItemFacet> {
            override fun compare(o1: PotionItemFacet, o2: PotionItemFacet): Int = compareValuesBy(o1, o2) { o ->
                o.itemStack.getPotionEffects()
                    .sortedByDescending { it.effectType.value().tier }
                    .mapInt { it.duration }
            }
        }

        private val StatusEffect.tier: Tier
            get() = GOOD_STATUS_EFFECT_TIER_LIST[this] ?: Tier.F

        private val GOOD_STATUS_EFFECT_TIER_LIST = hashMapOf(
            StatusEffects.INSTANT_HEALTH to Tier.S,

            StatusEffects.REGENERATION to Tier.A,
            StatusEffects.RESISTANCE to Tier.A,
            StatusEffects.FIRE_RESISTANCE to Tier.A,
            StatusEffects.HEALTH_BOOST to Tier.A,
            StatusEffects.ABSORPTION to Tier.A,

            StatusEffects.SPEED to Tier.B,
            StatusEffects.STRENGTH to Tier.B,
            StatusEffects.SLOW_FALLING to Tier.B,
            StatusEffects.INVISIBILITY to Tier.B,

            StatusEffects.SATURATION to Tier.C,
            StatusEffects.WATER_BREATHING to Tier.C,
            StatusEffects.JUMP_BOOST to Tier.C,
            StatusEffects.HASTE to Tier.C,
            StatusEffects.NIGHT_VISION to Tier.C,

            StatusEffects.LUCK to Tier.D,
        ).mapKeys { it.key.value() }

        val BAD_STATUS_EFFECTS = hashSetOf(
            StatusEffects.SLOWNESS,
            StatusEffects.MINING_FATIGUE,
            StatusEffects.INSTANT_DAMAGE,
            StatusEffects.NAUSEA,
            StatusEffects.BLINDNESS,
            StatusEffects.HUNGER,
            StatusEffects.WEAKNESS,
            StatusEffects.POISON,
            StatusEffects.WITHER,
            StatusEffects.GLOWING,
            StatusEffects.LEVITATION,
            StatusEffects.UNLUCK,
            StatusEffects.BAD_OMEN,
            StatusEffects.DARKNESS,
        )

        val GOOD_STATUS_EFFECTS = hashSetOf(
            StatusEffects.SPEED,
            StatusEffects.HASTE,
            StatusEffects.STRENGTH,
            StatusEffects.INSTANT_HEALTH,
            StatusEffects.JUMP_BOOST,
            StatusEffects.REGENERATION,
            StatusEffects.RESISTANCE,
            StatusEffects.FIRE_RESISTANCE,
            StatusEffects.WATER_BREATHING,
            StatusEffects.NIGHT_VISION,
            StatusEffects.HEALTH_BOOST,
            StatusEffects.ABSORPTION,
            StatusEffects.SATURATION,
            StatusEffects.LUCK,
            StatusEffects.SLOW_FALLING,
            StatusEffects.CONDUIT_POWER,
            StatusEffects.DOLPHINS_GRACE,
            StatusEffects.HERO_OF_THE_VILLAGE,
            StatusEffects.INVISIBILITY,
        )
    }

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as PotionItemFacet)
    }
}
