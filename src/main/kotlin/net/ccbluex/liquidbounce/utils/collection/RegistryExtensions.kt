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

package net.ccbluex.liquidbounce.utils.collection

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import java.util.SortedSet

fun <T : Any> Registry<T>.asComparator(): Comparator<T> = compareBy(this::getKey)

private val ITEM_REGISTRY_COMPARATOR = BuiltInRegistries.ITEM.asComparator()
private val BLOCK_REGISTRY_COMPARATOR = BuiltInRegistries.BLOCK.asComparator()

fun itemSortedSetOf(): SortedSet<Item> = ObjectRBTreeSet(ITEM_REGISTRY_COMPARATOR)

fun itemSortedSetOf(vararg items: Item): SortedSet<Item> =
    ObjectRBTreeSet(items, ITEM_REGISTRY_COMPARATOR)

fun blockSortedSetOf(): SortedSet<Block> = ObjectRBTreeSet(BLOCK_REGISTRY_COMPARATOR)

fun blockSortedSetOf(vararg blocks: Block): SortedSet<Block> =
    ObjectRBTreeSet(blocks, BLOCK_REGISTRY_COMPARATOR)
