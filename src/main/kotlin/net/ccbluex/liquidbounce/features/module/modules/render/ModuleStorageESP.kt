/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.block.Region
import net.ccbluex.liquidbounce.utils.block.WorldChangeNotifier
import net.minecraft.block.entity.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import java.awt.Color

/**
 * StorageESP module
 *
 * Allows you to see chests, dispensers, etc. through walls.
 */

object ModuleStorageESP : Module("StorageESP", Category.RENDER) {
//    private val modeValue = Choi("Mode", arrayOf("Box", "OtherBox", "Outline", "ShaderOutline", "ShaderGlow", "2D", "WireFrame"), "Outline")

    private val modes = choices("Mode", Box, arrayOf(Box))

    val chestValue by boolean("Chest", true)
    val enderChestValue by boolean("EnderChest", true)
    val furnaceValue by boolean("Furnace", true)
    val dispenserValue by boolean("Dispenser", true)
    val hopperValue by boolean("Hopper", true)
    val shulkerBoxValue by boolean("ShulkerBox", true)

    private val locations = HashMap<BlockPos, ChestType>()

    init {
        WorldChangeNotifier.subscribe(StorageScanner)
    }

    private object Box : Choice("Box") {

        override val parent: ChoiceConfigurable
            get() = modes

        private val outline by boolean("Outline", true)

        // todo: use box of block, not hardcoded
        private val box = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val blocksToRender = locations.entries.filter { it.value.shouldRender(it.key) }

            renderEnvironmentForWorld(matrixStack) {
                for ((pos, type) in blocksToRender) {
                    val color = type.color

                    val vec3 = Vec3(pos)

                    val baseColor = color.alpha(50)
                    val outlineColor = color.alpha(100)

                    withPosition(vec3) {
                        withColor(baseColor) {
                            drawSolidBox(box)
                        }

                        if (outline) {
                            withColor(outlineColor) {
                                drawOutlinedBox(box)
                            }
                        }
                    }
                }
            }
        }

    }

    private fun categorizeBlockEntity(block: BlockEntity): ChestType? {
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

    enum class ChestType(val color: Color4b, val shouldRender: (BlockPos) -> Boolean) {
        CHEST(Color4b(0, 66, 255), { chestValue && !net.ccbluex.liquidbounce.features.module.modules.world.ModuleChestAura.clickedBlocks.contains(it) }),
        ENDER_CHEST(Color4b(Color.MAGENTA), { enderChestValue && !net.ccbluex.liquidbounce.features.module.modules.world.ModuleChestAura.clickedBlocks.contains(it) }),
        FURNACE(Color4b(Color.BLACK), { furnaceValue }),
        DISPENSER(Color4b(Color.BLACK), { dispenserValue }),
        HOPPER(Color4b(Color.GRAY), { hopperValue }),
        SHULKER_BOX(Color4b(Color(0x6e, 0x4d, 0x6e).brighter()), { shulkerBoxValue })
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
