package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.Blocks
import net.minecraft.util.shape.VoxelShapes

object ModuleLiquidWalk : Module("LiquidWalk", Category.MOVEMENT) {

    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.state.block == Blocks.WATER) {
            event.shape = VoxelShapes.fullCube()
        } else if (event.state.block == Blocks.LAVA) {
            event.shape = VoxelShapes.fullCube()
        }
    }

}
