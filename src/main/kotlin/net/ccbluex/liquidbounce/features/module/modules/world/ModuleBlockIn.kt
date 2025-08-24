/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import kotlin.random.Random

/**
 * BlockIn module
 *
 * Builds blocks to cover yourself.
 */
object ModuleBlockIn : ClientModule("BlockIn", Category.WORLD, disableOnQuit = true) {

    private val blockPlacer = tree(BlockPlacer("Placer", this, Priority.NORMAL, ::slotFinder))
    private val autoDisable by boolean("AutoDisable", true)
    private val placeOrder = choices("PlaceOrder", 0) {
        arrayOf(Order.Normal, Order.Random, Order.BottomTop, Order.TopBottom)
    }
    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val blocks by blocks("Blocks", hashSetOf())

    private sealed class Order(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = placeOrder

        abstract fun positions(): MutableSet<BlockPos>

        object Normal : Order("Normal") {
            override fun positions(): ObjectArraySet<BlockPos> {
                val playerHeight = MathHelper.ceil(player.height)
                val result = ObjectArraySet<BlockPos>(10)
                result += startPos.down()
                rotateSurroundings {
                    val value = startPos.offset(it)
                    repeat(playerHeight) { i ->
                        result += value.up(i)
                    }
                }
                result += startPos.up(playerHeight)

                return result
            }
        }

        object Random : Order("Random") {
            override fun positions(): ObjectArraySet<BlockPos> {
                val array = Normal.positions().toArray()
                array.shuffle()
                return ObjectArraySet(array)
            }
        }

        object BottomTop : Order("BottomTop") {
            override fun positions(): MutableSet<BlockPos> {
                val array = Normal.positions().toArray()
                array.sortBy { (it as BlockPos).y }
                return ObjectArraySet(array)
            }
        }

        object TopBottom : Order("TopBottom") {
            override fun positions(): MutableSet<BlockPos> {
                val array = Normal.positions().toArray()
                array.sortByDescending { (it as BlockPos).y }
                return ObjectArraySet(array)
            }
        }

    }

    private val startPos = BlockPos.Mutable()
    private var rotateClockwise = false
    private var blockList = emptySet<BlockPos>()

    override fun onDisabled() {
        startPos.set(BlockPos.ORIGIN)
        blockList = emptySet()
        blockPlacer.disable()
    }

    override fun onEnabled() {
        startPos.set(player.blockPos)
        rotateClockwise = Random.nextBoolean()
        getPositions()
    }

    private inline fun rotateSurroundings(action: (Direction) -> Unit) {
        var direction = player.horizontalFacing
        repeat(4) {
            action(direction)
            // Next direction
            direction = if (rotateClockwise) {
                direction.rotateYClockwise()
            } else {
                direction.rotateYCounterclockwise()
            }
        }
    }

    private fun getPositions() {
        blockList = placeOrder.activeChoice.positions()
        debugParameter("Place Count") { blockList.size }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        blockPlacer.update(blockList)
        waitUntil(blockPlacer::isDone)

        if (autoDisable) {
            notification(name, message("filled"), NotificationEvent.Severity.SUCCESS)
            enabled = false
        }
        getPositions()
    }

    @Suppress("unused")
    private val movementHandler = handler<PlayerMovementTickEvent> {
        val currentPos = player.blockPos

        if (currentPos != startPos && currentPos != startPos.up()) {
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
            blockSlots.maxByOrNull { (_, block) -> block.hardness }
        } else {
            blockSlots.minByOrNull { (_, block) -> block.hardness }
        }?.first
    }

}
