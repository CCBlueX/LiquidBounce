package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.CobwebBlock
import net.minecraft.block.SnowBlock
import net.minecraft.util.shape.VoxelShapes

object ModuleBlockWalk : Module("BlockWalk", Category.MOVEMENT) {

    private val web by boolean("Cobweb", true)
    private val snow by boolean("Snow", true)

    val shapeHandler = handler<BlockShapeEvent> { event ->
        // For some reason, it doesn't really work even though it should? Does this need mixins too?
        event.shape = when(event.state.block) {
            is CobwebBlock -> if(web) VoxelShapes.fullCube() else return@handler
            is SnowBlock -> if(snow) VoxelShapes.fullCube() else return@handler
            else -> return@handler
        }
    }
}
