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
package net.ccbluex.liquidbounce.features.module.modules.world

import it.unimi.dsi.fastutil.objects.ObjectArraySet
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import kotlin.random.Random

/**
 * BlockIn module
 *
 * Builds blocks to cover yourself.
 */
object ModuleBlockIn : ClientModule("BlockIn", ModuleCategories.WORLD, disableOnQuit = true) {

    private val blockPlacer = tree(BlockPlacer("Placer", this, Priority.NORMAL, ::slotFinder))
    private val autoDisable by boolean("AutoDisable", true)
    private val placeOrder by enumChoice("PlaceOrder", Order.Normal)
    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val blocks by blocks("Blocks", blockSortedSetOf())

    private enum class Order(override val tag: String) : Tagged {

        Normal("Normal") {
            override fun positions(): Array<BlockPos> {
                val playerHeight = Mth.ceil(player.bbHeight)
                val result = ObjectArraySet<BlockPos>(10)
                result += startPos.below()
                rotateSurroundings {
                    val value = startPos.relative(it)
                    repeat(playerHeight) { i ->
                        result += value.above(i)
                    }
                }
                result += startPos.above(playerHeight)

                return result.toTypedArray()
            }
        },

        Random("Random") {
            override fun positions(): Array<BlockPos> {
                val array = Normal.positions()
                array.shuffle()
                return array
            }
        },

        BottomTop("BottomTop") {
            override fun positions(): Array<BlockPos> {
                val array = Normal.positions()
                array.sortBy { it.y }
                return array
            }
        },

        TopBottom("TopBottom") {
            override fun positions(): Array<BlockPos> {
                val array = Normal.positions()
                array.sortByDescending { it.y }
                return array
            }
        };

        abstract fun positions(): Array<BlockPos>
    }

    private val startPos = BlockPos.MutableBlockPos()
    private var rotateClockwise = false
    private var blockList = emptyList<BlockPos>()

    override fun onDisabled() {
        startPos.set(BlockPos.ZERO)
        blockList = emptyList()
        blockPlacer.disable()
    }

    override fun onEnabled() {
        startPos.set(player.blockPosition())
        rotateClockwise = Random.nextBoolean()
        getPositions()
    }

    private inline fun rotateSurroundings(action: (Direction) -> Unit) {
        var direction = player.direction
        repeat(4) {
            action(direction)
            // Next direction
            direction = if (rotateClockwise) {
                direction.clockWise
            } else {
                direction.counterClockWise
            }
        }
    }

    private fun getPositions() {
        blockList = placeOrder.positions().asList()
        debugParameter("Place Count") { blockList.size }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        blockPlacer.update(blockList)
        tickUntil { blockPlacer.isDone() }

        if (autoDisable) {
            notification(name, message("filled"), NotificationEvent.Severity.SUCCESS)
            enabled = false
        }
        getPositions()
    }

    @Suppress("unused")
    private val movementHandler = handler<PlayerMovementTickEvent> {
        val currentPos = player.blockPosition()

        if (currentPos != startPos && currentPos != startPos.above()) {
            notification(name, message("positionChanged"), NotificationEvent.Severity.ERROR)
            enabled = false
        }
    }

    @JvmStatic
    private fun slotFinder(pos: BlockPos?): HotbarItemSlot? {
        val blockSlots = Slots.OffhandWithHotbar.mapNotNull {
            it to (it.itemStack.getBlock()?.takeIf { b -> filter(b, blocks) } ?: return@mapNotNull null)
        }

        return if (pos in blockList) {
            blockSlots.maxByOrNull { (_, block) -> block.defaultDestroyTime() }
        } else {
            blockSlots.minByOrNull { (_, block) -> block.defaultDestroyTime() }
        }?.first
    }

}
