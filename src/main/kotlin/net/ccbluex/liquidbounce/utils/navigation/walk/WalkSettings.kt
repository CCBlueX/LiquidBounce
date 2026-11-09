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
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.navigation.flight.FlightAbilities
import net.ccbluex.liquidbounce.utils.navigation.flight.GlideAbilities
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * What a walking navigator may do on its way, as settings of [values].
 */
class WalkSettings(
    values: ValueGroup,
    allowBreak: Boolean,
    allowPlace: Boolean,
    allowParkour: Boolean,
) {

    val allowBreak by values.boolean("AllowBreak", allowBreak)
    val allowPlace by values.boolean("AllowPlace", allowPlace)
    private val allowParkour by values.boolean("AllowParkour", allowParkour)
    private val riskyJumps by values.boolean("RiskyJumps", false)
    private val maxFall by values.int("MaxFall", 3, 0..20, "blocks")
    private val pathRange by values.int("PathRange", 64, 16..128, "blocks")
    private val allowFly by values.boolean("AllowFly", true)
    private val allowElytra by values.boolean("AllowElytra", true)
    private val elytraRockets by values.boolean("ElytraRockets", true)

    /**
     * Longest route searched, in blocks along the path.
     */
    val maxLength: Double
        get() = pathRange.toDouble()

    /**
     * Highest drop onto ground, in blocks. MaxFall counts on top of a safe fall distance raised above the vanilla one.
     */
    val safeFall: Int
        get() {
            val safeFallDistance = floor(player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)).toInt()
            return maxFall + max(0, safeFallDistance - VANILLA_SAFE_FALL)
        }

    /**
     * Captures the abilities for one search. Call on the render thread; the result is safe to read from any thread.
     */
    fun capture(avoid: LongSet): WalkAbilities {
        val placeableBlocks = if (allowPlace) {
            Slots.OffhandWithHotbar.sumOf { slot ->
                val stack = slot.itemStack
                if (ScaffoldBlockItemSelection.isValidBlock(stack)) stack.count else 0
            }
        } else {
            0
        }

        return WalkAbilities(
            canBreak = allowBreak,
            canPlace = allowPlace && placeableBlocks > 0,
            placeableBlocks = placeableBlocks,
            canParkour = allowParkour,
            riskyJumps = allowParkour && riskyJumps,
            canSprint = player.foodData.foodLevel > MIN_SPRINT_FOOD,
            safeFall = safeFall,
            waterFall = WATER_FALL,
            breakTicks = if (allowBreak) BreakTimes.capture() else { _ -> Double.POSITIVE_INFINITY },
            avoid = LongOpenHashSet(avoid),
            air = player.airSupply,
            motion = MotionTraits.capture(player),
        )
    }

    /**
     * How the player can fly now, or null when it walks. Call on the render thread.
     */
    fun captureFlight(): FlightAbilities? = if (allowFly) FlightAbilities.capture() else null

    /**
     * How the player can glide now, or null without a usable elytra. Call on the render thread.
     */
    fun captureGlide(): GlideAbilities? = if (allowElytra) GlideAbilities.capture(elytraRockets) else null

    private companion object {
        const val VANILLA_SAFE_FALL = 3
        const val WATER_FALL = 23
        const val MIN_SPRINT_FOOD = 6
    }

}

/**
 * Ticks to mine a block with the best of copied hotbar tools, without haste, fatigue or water penalties.
 */
private class BreakTimes(private val tools: List<Tool>) : (BlockState) -> Double {

    class Tool(val stack: ItemStack, val efficiency: Int)

    override fun invoke(state: BlockState): Double {
        val hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
        if (hardness < 0f) {
            return Double.POSITIVE_INFINITY
        }
        if (hardness == 0f) {
            return 1.0
        }

        var best = Double.POSITIVE_INFINITY
        for (tool in tools) {
            val divisor = if (!state.requiresCorrectToolForDrops() || tool.stack.isCorrectToolForDrops(state)) {
                CORRECT_TOOL_DIVISOR
            } else {
                WRONG_TOOL_DIVISOR
            }

            val progress = speed(tool, state) / hardness / divisor
            if (progress > 0.0) {
                best = minOf(best, ceil(1.0 / progress))
            }
        }

        return best
    }

    /**
     * Same as [net.ccbluex.liquidbounce.utils.item.getDestroySpeedWithEnchantment], with the enchantment level read
     * beforehand so no registry is touched off the render thread.
     */
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

        fun capture(): BreakTimes {
            // The empty hand mines too, at speed 1.
            val tools = mutableListOf(Tool(ItemStack.EMPTY, 0))
            for (slot in Slots.Hotbar) {
                val stack = slot.itemStack
                if (!stack.isEmpty) {
                    tools += Tool(stack.copy(), stack.getEnchantment(Enchantments.EFFICIENCY))
                }
            }
            return BreakTimes(tools)
        }
    }

}
