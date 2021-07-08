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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleChestAura
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.utils.drawBoxNew
import net.ccbluex.liquidbounce.render.utils.drawBoxOutlineNew
import net.minecraft.block.entity.*
import net.minecraft.util.math.Box
import java.awt.Color

object ModuleStorageESP : Module("StorageESP", Category.RENDER) {
//    private val modeValue = Choi("Mode", arrayOf("Box", "OtherBox", "Outline", "ShaderOutline", "ShaderGlow", "2D", "WireFrame"), "Outline")

    private val modes = choices("Mode", Box, arrayOf(Box))

    private val chestValue by boolean("Chest", true)
    private val enderChestValue by boolean("EnderChest", true)
    private val furnaceValue by boolean("Furnace", true)
    private val dispenserValue by boolean("Dispenser", true)
    private val hopperValue by boolean("Hopper", true)
    private val shulkerBoxValue by boolean("ShulkerBox", true)

    private object Box : Choice("Box",) {

        override val parent: ChoiceConfigurable
            get() = modes

        private val outline by boolean("Outline", true)

        val box = drawBoxNew(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), Color4b.WHITE)

        val boxOutline = drawBoxOutlineNew(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), Color4b.WHITE)

        val tickHandler = handler<EngineRenderEvent> { event ->
//            val blockEntities = world.blockEntities
//
//            val instanceBuffer = PositionColorVertexFormat()
//            val instanceBufferOutline = PositionColorVertexFormat()
//
//            instanceBuffer.initBuffer(blockEntities.size)
//            instanceBufferOutline.initBuffer(blockEntities.size)
//
//            for (blockEntity in blockEntities) {
//                val base = getColor(blockEntity) ?: continue
//
//                val baseColor = Color4b(base.r, base.g, base.b, 50)
//                val outlineColor = Color4b(base.r, base.g, base.b, 100)
//
//                val pos = Vec3(blockEntity.pos)
//
//                instanceBuffer.putVertex { this.position = pos; this.color = baseColor }
//                instanceBufferOutline.putVertex { this.position = pos; this.color = outlineColor }
//            }

//            RenderEngine.enqueueForRendering(
//                RenderEngine.CAMERA_VIEW_LAYER,
//                espBoxInstancedRenderTask(instanceBuffer, box.first, box.second)
//            )
//            RenderEngine.enqueueForRendering(
//                RenderEngine.CAMERA_VIEW_LAYER,
//                espBoxInstancedOutlineRenderTask(instanceBufferOutline, boxOutline.first, boxOutline.second)
//            )
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
