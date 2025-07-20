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
 *
 */

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape

@Nameable("worldChange")
class WorldChangeEvent(val world: ClientWorld?) : Event()

@Nameable("chunkUnload")
class ChunkUnloadEvent(val x: Int, val z: Int) : Event()

@Nameable("chunkLoad")
class ChunkLoadEvent(val x: Int, val z: Int) : Event()

@Nameable("chunkDeltaUpdate")
class ChunkDeltaUpdateEvent(val x: Int, val z: Int) : Event()

@Nameable("blockChange")
class BlockChangeEvent(val blockPos: BlockPos, val newState: BlockState) : Event()

@Nameable("blockShape")
class BlockShapeEvent(var state: BlockState, var pos: BlockPos, var shape: VoxelShape) : Event()

@Nameable("blockBreakingProgress")
class BlockBreakingProgressEvent(val pos: BlockPos) : Event()

@Nameable("blockBreakingProgress")
class BlockAttackEvent(val pos: BlockPos) : CancellableEvent()

@Nameable("blockVelocityMultiplier")
class BlockVelocityMultiplierEvent(val block: Block, var multiplier: Float) : Event()

@Nameable("blockSlipperinessMultiplier")
class BlockSlipperinessMultiplierEvent(val block: Block, var slipperiness: Float) : Event()

@Nameable("entityEquipmentChange")
class PlayerEquipmentChangeEvent(
    val player: PlayerEntity, val equipmentSlot: EquipmentSlot, val itemStack: ItemStack
) : Event()

@Nameable("fluidPush")
class FluidPushEvent : CancellableEvent()

@Nameable("worldEntityRemove")
class WorldEntityRemoveEvent(val entity: Entity) : Event()
