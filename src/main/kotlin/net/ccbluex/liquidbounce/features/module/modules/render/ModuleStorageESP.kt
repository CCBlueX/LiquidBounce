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
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.*
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.AbstractDonkeyEntity
import net.minecraft.entity.vehicle.ChestBoatEntity
import net.minecraft.entity.vehicle.HopperMinecartEntity
import net.minecraft.entity.vehicle.StorageMinecartEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color

/**
 * StorageESP module
 *
 * Allows you to see chests, dispensers, etc. through walls.
 */

object ModuleStorageESP : ClientModule("StorageESP", Category.RENDER, aliases = arrayOf("ChestESP")) {

    private val modes = choices("Mode", Glow, arrayOf(BoxMode, Glow))

    private val chestColor by color("Chest", Color4b(0, 100, 255))
    private val enderChestColor by color("EnderChest", Color4b(Color.MAGENTA))
    private val furnaceColor by color("Furnace", Color4b(79, 79, 79))
    private val dispenserColor by color("Dispenser", Color4b(Color.LIGHT_GRAY))
    private val hopperColor by color("Hopper", Color4b(Color.GRAY))
    private val shulkerColor by color("ShulkerBox", Color4b(Color(0x6e, 0x4d, 0x6e).brighter()))
    private val potColor by color("Pot", Color4b(209, 134, 0))

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

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val queuedBoxes = collectBoxesToDraw(event)

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    for ((pos, box, color) in queuedBoxes) {
                        val baseColor = color.with(a = 50)
                        val outlineColor = color.with(a = 100)

                        withPositionRelativeToCamera(pos) {
                            drawBox(box, baseColor, outlineColor.takeIf { outline })
                        }
                    }
                }
            }
        }

        @JvmRecord
        private data class BoxRecord(val pos: Vec3d, val box: Box, val color: Color4b)

        private fun collectBoxesToDraw(event: WorldRenderEvent): List<BoxRecord> {
            val queuedBoxes = mutableListOf<BoxRecord>()

            for ((pos, type) in StorageScanner.iterate()) {
                val color = type.color

                if (color.a <= 0 || !type.shouldRender(pos)) {
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

                queuedBoxes.add(BoxRecord(pos.toVec3d(), boundingBox, color))
            }

            for (entity in world.entities) {
                val type = entity.categorize() ?: continue

                val pos = entity.interpolateCurrentPosition(event.partialTicks)

                val dimensions = entity.getDimensions(entity.pose)
                val d = dimensions.width.toDouble() / 2.0
                val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)

                queuedBoxes.add(BoxRecord(pos, box, type.color))
            }

            return queuedBoxes

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
                BoxRenderer.drawWith(this) {
                    for ((pos, type) in StorageScanner.iterate()) {
                        val state = pos.getState() ?: continue

                        // non-model blocks are already processed by WorldRenderer where we injected code which renders
                        // their outline
                        if (state.renderType != BlockRenderType.MODEL) {
                            continue
                        }

                        if (state.isAir) {
                            continue
                        }

                        val outlineShape = state.getOutlineShape(world, pos)

                        val boundingBox = if (outlineShape.isEmpty) {
                            FULL_BOX
                        } else {
                            outlineShape.boundingBox
                        }

                        withPosition(relativeToCamera(Vec3d.of(pos))) {
                            drawBox(boundingBox, type.color)
                        }

                        event.markDirty()
                    }
                }
            }
        }
    }

    @JvmStatic
    fun Entity.categorize(): ChestType? {
        return when (this) {
            // This includes any storage type minecart entity including ChestMinecartEntity
            is HopperMinecartEntity -> ChestType.HOPPER
            is StorageMinecartEntity -> ChestType.CHEST
            is ChestBoatEntity -> ChestType.CHEST
            is AbstractDonkeyEntity -> ChestType.CHEST.takeIf { hasChest() }
            else -> null
        }
    }

    @JvmStatic
    fun BlockEntity.categorize(): ChestType? {
        return when (this) {
            is ChestBlockEntity -> ChestType.CHEST
            is EnderChestBlockEntity -> ChestType.ENDER_CHEST
            is AbstractFurnaceBlockEntity -> ChestType.FURNACE
            is DispenserBlockEntity -> ChestType.DISPENSER
            is HopperBlockEntity -> ChestType.HOPPER
            is ShulkerBoxBlockEntity -> ChestType.SHULKER_BOX
            is BarrelBlockEntity -> ChestType.CHEST
            is DecoratedPotBlockEntity -> ChestType.POT
            else -> null
        }
    }

    enum class ChestType {
        CHEST {
            override val color get() = chestColor

            override fun shouldRender(pos: BlockPos) = pos !in FeatureChestAura.interactedBlocksSet
        },
        ENDER_CHEST {
            override val color get() = enderChestColor

            override fun shouldRender(pos: BlockPos) = pos !in FeatureChestAura.interactedBlocksSet
        },
        FURNACE {
            override val color get() = furnaceColor
        },
        DISPENSER {
            override val color get() = dispenserColor
        },
        HOPPER {
            override val color get() = hopperColor
        },
        SHULKER_BOX {
            override val color get() = shulkerColor

            override fun shouldRender(pos: BlockPos) = pos !in FeatureChestAura.interactedBlocksSet
        },
        POT {
            override val color get() = potColor
        };

        abstract val color: Color4b

        open fun shouldRender(pos: BlockPos): Boolean = true
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
