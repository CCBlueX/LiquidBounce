/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.fastutil.asIntList
import net.ccbluex.fastutil.mapToIntArray
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.Tier
import net.minecraft.core.Holder
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.LingeringPotionItem
import net.minecraft.world.item.PotionItem
import net.minecraft.world.item.SplashPotionItem

class PotionItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    override val category: ItemCategory
        get() = ItemType.POTION.defaultCategory

    companion object {
        /**
         * Prefers potions which have more status effects of higher Tier.
         * For example:
         * - `S > A`
         * - `A + A > A + B`
         * - `A + A + F > A + A`
         * - etc.
         */
        private val PreferHigherTierPotions = Comparator<PotionItemFacet> { o1, o2 ->
            compareValuesBy(o1, o2) { o ->
                o.itemStack.getPotionEffects()
                    .mapTo(ObjectArrayList()) { it.effect.value().tier }
                    .apply { sortDescending() }
            }
        }

        /**
         * This check is pretty random as it does not care which effect it compares.
         * - Anything (S-Tier) II + Anything (S-Tier) I > Anything (S-Tier) I + Anything (S-Tier) I
         */
        private val PreferAmplifier = Comparator<PotionItemFacet> { o1, o2 ->
            compareValuesBy(o1, o2) { o ->
                o.itemStack.getPotionEffects()
                    .sortedByDescending { it.effect.value().tier }
                    .mapToIntArray { it.amplifier }.asIntList()
            }
        }

        /**
         * Prefers quick and targeted potions: `splash potion > drinkable potion > lingering potion`
         */
        private val PreferSplashPotions = Comparator<PotionItemFacet> { o1, o2 ->
            fun tierOfPotionType(potionItem: PotionItem): Tier {
                return when (potionItem) {
                    is SplashPotionItem -> Tier.S
                    is LingeringPotionItem -> Tier.B
                    else -> Tier.A
                }
            }

            compareValuesBy(o1, o2) { o ->
                tierOfPotionType(o.itemStack.item as PotionItem)
            }
        }

        /**
         * Prefers higher duration of higher tiers.
         * - `S (1:00) > S (0:30)`
         * - `S (0:30) + A (1:00) > S (1:00) + A (20:00)`
         */
        private val PreferHigherDurationPotions = Comparator<PotionItemFacet> { o1, o2 ->
            compareValuesBy(o1, o2) { o ->
                o.itemStack.getPotionEffects()
                    .sortedByDescending { it.effect.value().tier }
                    .mapToIntArray { it.duration }.asIntList()
            }
        }

        private val COMPARATOR = ComparatorChain(
            PreferHigherTierPotions,
            PreferAmplifier,
            PreferSplashPotions,
            PreferHigherDurationPotions,
            PREFER_ITEMS_IN_HOTBAR,
            STABILIZE_COMPARISON
        )

        private val MobEffect.tier: Tier
            get() = GOOD_STATUS_EFFECT_TIER_LIST.getOrDefault(this, Tier.F)

        private val GOOD_STATUS_EFFECT_TIER_LIST = hashMapOf(
            MobEffects.INSTANT_HEALTH to Tier.S,

            MobEffects.REGENERATION to Tier.A,
            MobEffects.RESISTANCE to Tier.A,
            MobEffects.FIRE_RESISTANCE to Tier.A,
            MobEffects.HEALTH_BOOST to Tier.A,
            MobEffects.ABSORPTION to Tier.A,

            MobEffects.SPEED to Tier.B,
            MobEffects.STRENGTH to Tier.B,
            MobEffects.SLOW_FALLING to Tier.B,
            MobEffects.INVISIBILITY to Tier.B,

            MobEffects.SATURATION to Tier.C,
            MobEffects.WATER_BREATHING to Tier.C,
            MobEffects.JUMP_BOOST to Tier.C,
            MobEffects.HASTE to Tier.C,
            MobEffects.NIGHT_VISION to Tier.C,

            MobEffects.LUCK to Tier.D,
        ).mapKeys { it.key.value() }

        @JvmField
        val BAD_STATUS_EFFECTS: Set<Holder<MobEffect>> = ReferenceOpenHashSet.of(
            MobEffects.SLOWNESS,
            MobEffects.MINING_FATIGUE,
            MobEffects.INSTANT_DAMAGE,
            MobEffects.NAUSEA,
            MobEffects.BLINDNESS,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.GLOWING,
            MobEffects.LEVITATION,
            MobEffects.UNLUCK,
            MobEffects.BAD_OMEN,
            MobEffects.DARKNESS,
        )

        @JvmField
        val GOOD_STATUS_EFFECTS: Set<Holder<MobEffect>> = ReferenceOpenHashSet.of(
            MobEffects.SPEED,
            MobEffects.HASTE,
            MobEffects.STRENGTH,
            MobEffects.INSTANT_HEALTH,
            MobEffects.JUMP_BOOST,
            MobEffects.REGENERATION,
            MobEffects.RESISTANCE,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.WATER_BREATHING,
            MobEffects.NIGHT_VISION,
            MobEffects.HEALTH_BOOST,
            MobEffects.ABSORPTION,
            MobEffects.SATURATION,
            MobEffects.LUCK,
            MobEffects.SLOW_FALLING,
            MobEffects.CONDUIT_POWER,
            MobEffects.DOLPHINS_GRACE,
            MobEffects.HERO_OF_THE_VILLAGE,
            MobEffects.INVISIBILITY,
        )
    }

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as PotionItemFacet)
    }
}
