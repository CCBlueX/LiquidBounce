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

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.annotations.Nameable
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.VoxelShape

@Nameable("worldChange")
class WorldChangeEvent(val world: ClientLevel?) : Event()

@Nameable("chunkUnload")
class ChunkUnloadEvent(val pos: ChunkPos) : Event()

@Nameable("chunkLoad")
class ChunkLoadEvent(val x: Int, val z: Int) : Event()

@Nameable("chunkDeltaUpdate")
class ChunkDeltaUpdateEvent(val packet: ClientboundSectionBlocksUpdatePacket) : Event()

@Nameable("blockChange")
class BlockChangeEvent(val blockPos: BlockPos, val newState: BlockState) : Event()

@Nameable("blockShape")
class BlockShapeEvent(var state: BlockState, var pos: BlockPos, var shape: VoxelShape) : Event()

@Nameable("blockBreakingProgress")
class BlockBreakingProgressEvent(val pos: BlockPos) : Event()

@Nameable("blockAttack")
class BlockAttackEvent(val pos: BlockPos) : CancellableEvent()

@Nameable("blockVelocityMultiplier")
class BlockVelocityMultiplierEvent(val block: Block, var multiplier: Float) : Event()

@Nameable("blockSlipperinessMultiplier")
class BlockSlipperinessMultiplierEvent(val block: Block, var slipperiness: Float) : Event()

@Nameable("entityEquipmentChange")
class EntityEquipmentChangeEvent(
    val entity: LivingEntity, val equipmentSlot: EquipmentSlot, val itemStack: ItemStack
) : Event()

@Nameable("fluidPush")
class FluidPushEvent : CancellableEvent()

@Nameable("worldEntityRemove")
class WorldEntityRemoveEvent(val entity: Entity) : Event()
