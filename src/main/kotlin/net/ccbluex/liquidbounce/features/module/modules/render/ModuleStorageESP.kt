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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.*
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.AbstractDonkeyEntity
import net.minecraft.entity.vehicle.ChestBoatEntity
import net.minecraft.entity.vehicle.ChestRaftEntity
import net.minecraft.entity.vehicle.HopperMinecartEntity
import net.minecraft.entity.vehicle.StorageMinecartEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import java.awt.Color

/**
 * StorageESP module
 *
 * Allows you to see chests, dispensers, etc. through walls.
 */

object ModuleStorageESP : ClientModule("StorageESP", Category.RENDER, aliases = listOf("ChestESP")) {

    private val modes = choices("Mode", Glow, arrayOf(BoxMode, Glow))

    sealed class ChestType(name: String, defaultColor: Color4b) : ToggleableConfigurable(this, name, enabled = true) {
        val color by color("Color", defaultColor)
        val tracers by boolean("Tracers", false)

        fun shouldRender(pos: BlockPos): Boolean = pos !in FeatureChestAura.interactedBlocksSet

        object Chest : ChestType("Chest", Color4b(0, 100, 255))
        object EnderChest : ChestType("EnderChest", Color4b(Color.MAGENTA))
        object Furnace : ChestType("Furnace", Color4b(79, 79, 79))
        object BrewingStand : ChestType("BrewingStand", Color4b(139, 69, 19))
        object Dispenser : ChestType("Dispenser", Color4b(Color.LIGHT_GRAY))
        object Hopper : ChestType("Hopper", Color4b(Color.GRAY))
        object ShulkerBox : ChestType("ShulkerBox", Color4b(Color(0x6e, 0x4d, 0x6e).brighter()))
        object Pot : ChestType("Pot", Color4b(209, 134, 0))
    }

    init {
        tree(ChestType.Chest)
        tree(ChestType.EnderChest)
        tree(ChestType.Furnace)
        tree(ChestType.BrewingStand)
        tree(ChestType.Dispenser)
        tree(ChestType.Hopper)
        tree(ChestType.ShulkerBox)
        tree(ChestType.Pot)
    }

    private val requiresChestStealer by boolean("RequiresChestStealer", false)

    override fun onEnabled() {
        ChunkScanner.subscribe(StorageScanner)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(StorageScanner)
    }

    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        private val blockBoxes = mutableListOf<BlockBox>()
        private val entityBoxes = mutableListOf<EntityBox>()
        private val blockPos = BlockPos.Mutable()

        override fun disable() {
            blockBoxes.clear()
            entityBoxes.clear()
            super.disable()
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (blockBoxes.isEmpty() && entityBoxes.isEmpty()) return@handler

            val matrixStack = event.matrixStack

            renderEnvironmentForWorld(matrixStack) {
                startBatch()

                for ((pos, box, color) in blockBoxes) {
                    val baseColor = color.with(a = 50)
                    val outlineColor = if (outline) color.with(a = 100) else null

                    withPositionRelativeToCamera(blockPos.set(pos)) {
                        drawBox(box, baseColor, outlineColor)
                    }
                }

                for ((entity, box, color) in entityBoxes) {
                    val baseColor = color.with(a = 50)
                    val outlineColor = if (outline) color.with(a = 100) else null

                    val pos = entity.interpolateCurrentPosition(event.partialTicks)
                    withPositionRelativeToCamera(pos) {
                        drawBox(box, baseColor, outlineColor)
                    }
                }

                commitBatch()
            }
        }

        @JvmRecord
        private data class BlockBox(val pos: Long, val box: Box, val color: Color4b)

        @JvmRecord
        private data class EntityBox(val entity: Entity, val box: Box, val color: Color4b)

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            blockBoxes.clear()

            for ((pos, type) in StorageScanner.iterate()) {
                val color = type.color

                if (!type.enabled || color.isTransparent || !type.shouldRender(pos)) {
                    continue
                }

                val state = pos.getState()

                if (state == null || state.isAir) {
                    continue
                }

                val outlineShape = state.getOutlineShape(world, pos)
                val boundingBox = if (outlineShape.isEmpty) {
                    FULL_BOX
                } else {
                    outlineShape.boundingBox
                }

                blockBoxes.add(BlockBox(pos.asLong(), boundingBox, color))
            }

            entityBoxes.clear()

            for (entity in world.entities) {
                val type = entity.categorize()?.takeIf { it.enabled && !it.color.isTransparent } ?: continue

                val dimensions = entity.getDimensions(entity.pose)
                val d = dimensions.width.toDouble() / 2.0
                val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)

                entityBoxes.add(EntityBox(entity, box, type.color))
            }
        }

    }

    object Glow : Choice("Glow") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val glowRenderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW
                || StorageScanner.isEmpty()) {
                return@handler
            }

            renderEnvironmentForWorld(event.matrixStack) {
                // non-model blocks are already processed by WorldRenderer where we injected code which renders
                // their outline
                startBatch()
                for ((pos, type) in StorageScanner.iterate()) {
                    if (!type.enabled) continue

                    val state = pos.getState() ?: continue

                    // non-model blocks are already processed by WorldRenderer where we injected code which renders
                    // their outline
                    if (state.renderType != BlockRenderType.MODEL || state.isAir) {
                        continue
                    }

                    val outlineShape = state.getOutlineShape(world, pos)

                    val boundingBox = if (outlineShape.isEmpty) {
                        FULL_BOX
                    } else {
                        outlineShape.boundingBox
                    }

                    withPositionRelativeToCamera(pos) {
                        drawBox(boundingBox, type.color)
                    }

                    event.markDirty()
                }
                commitBatch()
            }
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (StorageScanner.isEmpty()) {
            return@handler
        }

        renderEnvironmentForWorld(event.matrixStack) {
            val eyeVector = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

            longLines {
                for ((blockPos, type) in StorageScanner.iterate()) {
                    if (!type.enabled || !type.tracers || type.color.a <= 0) continue
                    val pos = relativeToCamera(blockPos.toCenterPos()).toVec3()

                    drawLines(type.color.toARGB(), eyeVector, pos, pos)
                }
            }
        }
    }

    @JvmStatic
    fun Entity.categorize(): ChestType? {
        return when (this) {
            // This includes any storage type minecart entity including ChestMinecartEntity
            is HopperMinecartEntity -> ChestType.Hopper
            is StorageMinecartEntity -> ChestType.Chest
            is ChestBoatEntity -> ChestType.Chest
            is ChestRaftEntity -> ChestType.Chest
            is AbstractDonkeyEntity -> ChestType.Chest.takeIf { hasChest() }
            else -> null
        }
    }

    @JvmStatic
    fun BlockEntity.categorize(): ChestType? {
        return when (this) {
            is ChestBlockEntity, is BarrelBlockEntity -> ChestType.Chest
            is EnderChestBlockEntity -> ChestType.EnderChest
            is AbstractFurnaceBlockEntity -> ChestType.Furnace
            is BrewingStandBlockEntity -> ChestType.BrewingStand
            is DispenserBlockEntity -> ChestType.Dispenser
            is CrafterBlockEntity -> ChestType.Dispenser
            is HopperBlockEntity -> ChestType.Hopper
            is ShulkerBoxBlockEntity -> ChestType.ShulkerBox
            is DecoratedPotBlockEntity -> ChestType.Pot
            else -> null
        }
    }

    private object StorageScanner : AbstractBlockLocationTracker.State2BlockPos<ChestType>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): ChestType? {
            val chunk = mc.world?.getChunk(pos) ?: return null
            return chunk.getBlockEntity(pos)?.categorize()
        }
    }

    override val running: Boolean
        get() {
            if (requiresChestStealer && !ModuleChestStealer.running) {
                return false
            }

            return super.running
        }

}
