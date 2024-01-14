/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleChestAura
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.block.Region
import net.ccbluex.liquidbounce.utils.block.WorldChangeNotifier
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.block.entity.*
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.ChestBoatEntity
import net.minecraft.entity.vehicle.ChestMinecartEntity
import net.minecraft.entity.vehicle.StorageMinecartEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import java.awt.Color

/**
 * StorageESP module
 *
 * Allows you to see chests, dispensers, etc. through walls.
 */

object ModuleStorageESP : Module("StorageESP", Category.RENDER) {

    private val modes = choices("Mode", Glow, arrayOf(Box, Glow))

    private val chestColor by color("Chest", Color4b(16, 71, 92))
    private val enderChestColor by color("EnderChest", Color4b(Color.MAGENTA))
    private val furnaceColor by color("Furnace", Color4b(Color.BLACK))
    private val dispenserColor by color("Dispenser", Color4b(Color.BLACK))
    private val hopperColor by color("Hopper", Color4b(Color.GRAY))
    private val shulkerColor by color("ShulkerBox", Color4b(Color(0x6e, 0x4d, 0x6e).brighter()))

    private val locations = HashMap<BlockPos, ChestType>()

    init {
        WorldChangeNotifier.subscribe(StorageScanner)
    }

    private object Box : Choice("Box") {

        override val parent: ChoiceConfigurable
            get() = modes

        private val outline by boolean("Outline", true)
        private val fullBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val blocksToRender =
                locations.entries
                    .filter { (pos, type) -> type.color().a > 0 && type.shouldRender(pos) }
                    .groupBy {it.value}

            val entitiesToRender =
                world.entities
                    .filter { categorizeEntity(it) != null }
                    .groupBy { categorizeEntity(it)!! }

            renderEnvironmentForWorld(matrixStack) {
                for ((type, blocks) in blocksToRender) {
                    val boxRenderer = BoxesRenderer()

                    val color = type.color()
                    val baseColor = color.alpha(50)
                    val outlineColor = color.alpha(100)

                    for ((pos, _) in blocks) {
                        val vec3 = Vec3(pos)
                        val state = pos.getState() ?: continue

                        if (state.isAir) {
                            continue
                        }

                        val outlineShape = state.getOutlineShape(world, pos)
                        val boundingBox = if (outlineShape.isEmpty) {
                            fullBox
                        } else {
                            outlineShape.boundingBox
                        }

                        withPosition(vec3) {
                            boxRenderer.drawBox(this, boundingBox, outline)
                        }
                    }

                    boxRenderer.draw(this, baseColor, outlineColor)
                }

                for ((type, entities) in entitiesToRender) {
                    val boxRenderer = BoxesRenderer()

                    val color = type.color()
                    val baseColor = color.alpha(50)
                    val outlineColor = color.alpha(100)

                    for (entity in entities) {
                        val vec3 = entity.interpolateCurrentPosition(event.partialTicks).toVec3()
                        val dimensions = entity.getDimensions(entity.pose)
                        val d = dimensions.width.toDouble() / 2.0
                        val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)

                        withPosition(vec3) {
                            boxRenderer.drawBox(this, box, outline)
                        }
                    }

                    boxRenderer.draw(this, baseColor, outlineColor)
                }
            }
        }

    }

    object Glow : Choice("Glow") {

        override val parent: ChoiceConfigurable
            get() = modes

    }

    fun categorizeEntity(entity: Entity): ChestType? {
        return when (entity) {
            // This includes any storage type minecart entity including ChestMinecartEntity
            is StorageMinecartEntity -> ChestType.CHEST
            is ChestBoatEntity -> ChestType.CHEST
            else -> null
        }
    }

    fun categorizeBlockEntity(block: BlockEntity): ChestType? {
        return when (block) {
            is ChestBlockEntity -> ChestType.CHEST
            is EnderChestBlockEntity -> ChestType.ENDER_CHEST
            is FurnaceBlockEntity -> ChestType.FURNACE
            is DispenserBlockEntity -> ChestType.DISPENSER
            is HopperBlockEntity -> ChestType.HOPPER
            is ShulkerBoxBlockEntity -> ChestType.SHULKER_BOX
            is BarrelBlockEntity -> ChestType.CHEST
            else -> null
        }
    }

    enum class ChestType(val color: () -> Color4b, val shouldRender: (BlockPos) -> Boolean = { true }) {
        CHEST({chestColor}, { !ModuleChestAura.clickedBlocks.contains(it) }),
        ENDER_CHEST({enderChestColor}, { !ModuleChestAura.clickedBlocks.contains(it) }),
        FURNACE({furnaceColor}),
        DISPENSER({dispenserColor}),
        HOPPER({hopperColor}),
        SHULKER_BOX({ net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP.shulkerColor })
    }

    object StorageScanner : WorldChangeNotifier.WorldChangeSubscriber {
        override fun invalidate(region: Region, rescan: Boolean) {}

        override fun invalidateChunk(x: Int, z: Int, rescan: Boolean) {
            // Clean up all chests in this chunk
            locations.entries.removeIf { it.key.x shr 4 == x && it.key.z shr 4 == z }

            // Chunk was unloaded? Don't rescan then
            if (!rescan) {
                return
            }

            val chunk = world.getChunk(x, z)

            // Don't scan empty chunks (might be a noop)
            if (chunk.isEmpty) {
                return
            }

            for ((pos, blockEntity) in chunk.blockEntities.entries) {
                val type = categorizeBlockEntity(blockEntity) ?: continue

                locations[pos] = type
            }
        }

        override fun invalidateEverything() {
            locations.clear()
        }

    }

}
