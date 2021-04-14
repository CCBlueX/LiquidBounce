package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.Blocks
import net.minecraft.util.shape.VoxelShapes

object ModuleBlockWalk : Module("BlockWalk", Category.MOVEMENT) {

    private val blocks by blocks("Blocks", arrayListOf(Blocks.COBWEB, Blocks.SNOW))

    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.state.block in blocks) {
            event.shape = VoxelShapes.fullCube()
        }
    }
}
