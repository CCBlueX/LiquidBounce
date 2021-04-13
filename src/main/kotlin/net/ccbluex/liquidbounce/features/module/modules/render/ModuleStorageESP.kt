/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Choice
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleChestAura
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.utils.drawBox
import net.ccbluex.liquidbounce.render.utils.drawBoxOutline
import net.minecraft.block.entity.*
import net.minecraft.util.math.Box
import java.awt.Color

object ModuleStorageESP : Module("StorageESP", Category.RENDER) {
//    private val modeValue = Choi("Mode", arrayOf("Box", "OtherBox", "Outline", "ShaderOutline", "ShaderGlow", "2D", "WireFrame"), "Outline")

    private val modes = choices("Mode", "Box") {
        Box
    }

    private val chestValue by boolean("Chest", true)
    private val enderChestValue by boolean("EnderChest", true)
    private val furnaceValue by boolean("Furnace", true)
    private val dispenserValue by boolean("Dispenser", true)
    private val hopperValue by boolean("Hopper", true)
    private val shulkerBoxValue by boolean("ShulkerBox", true)


    private object Box : Choice("Box", modes) {
        private val outline by boolean("Outline", true)

        val box = run {
            val task = drawBox(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), Color4b.WHITE)

            task.storageType = VBOStorageType.Static

            task
        }

        val boxOutline = run {
            val task = drawBoxOutline(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), Color4b.WHITE)

            task.storageType = VBOStorageType.Static

            task
        }

        val tickHandler = handler<EngineRenderEvent> { event ->
            val blockEntities = world.blockEntities

            val renderTask = InstancedColoredPrimitiveRenderTask(blockEntities.size, box)
            val outlineRenderTask =
                if (outline) InstancedColoredPrimitiveRenderTask(blockEntities.size, boxOutline) else null

            for (blockEntity in blockEntities) {
                val base = getColor(blockEntity) ?: continue

                val baseColor = Color4b(base.r, base.g, base.b, 50)
                val outlineColor = Color4b(base.r, base.g, base.b, 100)

                val pos = Vec3(blockEntity.pos)

                renderTask.instance(pos, baseColor)
                outlineRenderTask?.instance(pos, outlineColor)
            }

            RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, renderTask)
            outlineRenderTask?.let { RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, it) }
        }

    }

    private fun getColor(block: BlockEntity): Color4b? {
        return when {
            chestValue && block is ChestBlockEntity && !ModuleChestAura.clickedBlocks.contains(block.pos) -> Color4b(
                0,
                66,
                255
            )
            enderChestValue && block is EnderChestBlockEntity && !ModuleChestAura.clickedBlocks.contains(block.pos) -> Color4b(
                Color.MAGENTA
            )
            furnaceValue && block is FurnaceBlockEntity -> Color4b(Color.BLACK)
            dispenserValue && block is DispenserBlockEntity -> Color4b(Color.BLACK)
            hopperValue && block is HopperBlockEntity -> Color4b(Color.GRAY)
            shulkerBoxValue && block is ShulkerBoxBlockEntity -> Color4b(Color(0x6e, 0x4d, 0x6e).brighter())
            else -> null
        }
    }


}
