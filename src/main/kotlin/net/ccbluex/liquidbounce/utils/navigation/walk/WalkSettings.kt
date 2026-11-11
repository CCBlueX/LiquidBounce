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

package net.ccbluex.liquidbounce.utils.navigation.walk

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.MotionTraits
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity.BASE_SAFE_FALL_DISTANCE
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.state.BlockState
import java.util.function.ToDoubleFunction
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** What a walking navigator may do on its way, as settings of [values]. */
class WalkSettings(
    values: ValueGroup,
    allowBreak: Boolean,
    allowPlace: Boolean,
    allowParkour: Boolean,
) {

    private val allowBreak by values.boolean("AllowBreak", allowBreak)
    private val allowPlace by values.boolean("AllowPlace", allowPlace)
    private val allowParkour by values.boolean("AllowParkour", allowParkour)
    private val riskyJumps by values.boolean("RiskyJumps", false)
    private val maxFall by values.int("MaxFall", 3, 0..20, "blocks")
    private val pathRange by values.int("PathRange", 64, 16..128, "blocks")
    internal val allowFly by values.boolean("AllowFly", true)
    internal val allowElytra by values.boolean("AllowElytra", true)
    internal val elytraRockets by values.boolean("ElytraRockets", true)

    /** Longest route searched, in blocks along the path. */
    val maxLength: Double
        get() = pathRange.toDouble()

    /** Highest drop onto ground, in blocks: MaxFall on top of what the safe fall distance has over the vanilla one. */
    val safeFall: Int
        get() {
            val safeFallDistance = floor(player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)).toInt()
            return maxFall + max(0, safeFallDistance - BASE_SAFE_FALL_DISTANCE)
        }

    /** The abilities for one search, safe to read from any thread. Captures run on the render thread. */
    fun capture(avoid: LongSet): Abilities {
        val placeableBlocks = if (allowPlace) countPlaceableBlocks() else 0
        return Abilities(
            canBreak = allowBreak,
            canPlace = placeableBlocks > 0,
            placeableBlocks = placeableBlocks,
            canParkour = allowParkour,
            riskyJumps = allowParkour && riskyJumps,
            canSprint = player.foodData.foodLevel > MIN_SPRINT_FOOD,
            safeFall = safeFall,
            waterFall = WATER_FALL,
            breakTicks = if (allowBreak) BreakTimes.capture() else ToDoubleFunction { Double.POSITIVE_INFINITY },
            avoid = LongOpenHashSet(avoid),
            air = player.airSupply,
            motion = MotionTraits.capture(player),
        )
    }

    private fun countPlaceableBlocks() = Slots.OffhandWithHotbar.sumOf { slot ->
        slot.itemStack.takeIf(ScaffoldBlockItemSelection::isValidBlock)?.count ?: 0
    }

    private companion object {
        const val WATER_FALL = 23
        const val MIN_SPRINT_FOOD = 6
    }

}

/** Ticks to mine a block with the best of the copied hotbar tools, without haste, fatigue or water penalties. */
private class BreakTimes(private val tools: List<Tool>) : ToDoubleFunction<BlockState> {

    class Tool(val stack: ItemStack, val efficiency: Int)

    override fun applyAsDouble(state: BlockState): Double {
        val hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
        return when {
            hardness < 0f -> Double.POSITIVE_INFINITY
            hardness == 0f -> 1.0
            else -> tools.minOf { ticks(it, state, hardness) }
        }
    }

    private fun ticks(tool: Tool, state: BlockState, hardness: Float): Double {
        val harvests = !state.requiresCorrectToolForDrops() || tool.stack.isCorrectToolForDrops(state)
        val divisor = if (harvests) CORRECT_TOOL_DIVISOR else WRONG_TOOL_DIVISOR
        val progress = speed(tool, state) / hardness / divisor
        return if (progress > 0.0) ceil(1.0 / progress) else Double.POSITIVE_INFINITY
    }

    /** As getDestroySpeedWithEnchantment, with the level read beforehand: registries are for the render thread. */
    private fun speed(tool: Tool, state: BlockState): Double {
        var speed = tool.stack.getDestroySpeed(state)
        if (speed > 1f && tool.efficiency != 0) {
            speed += (tool.efficiency * tool.efficiency + 1f).coerceIn(0f, MAX_EFFICIENCY_BONUS)
        }
        return speed.toDouble()
    }

    companion object {
        private const val CORRECT_TOOL_DIVISOR = 30.0
        private const val WRONG_TOOL_DIVISOR = 100.0
        private const val MAX_EFFICIENCY_BONUS = 1024f

        private val EMPTY_HAND = Tool(ItemStack.EMPTY, 0)

        fun capture(): BreakTimes {
            val stacks = Slots.Hotbar.map { it.itemStack }.filterNot { it.isEmpty }
            val tools = stacks.map { Tool(it.copy(), it.getEnchantment(Enchantments.EFFICIENCY)) }
            return BreakTimes(tools + EMPTY_HAND)
        }
    }

}
