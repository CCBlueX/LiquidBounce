package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos

object ModuleLadderBoost : Module("LadderBoost", Category.MOVEMENT) {

    val repeatable = repeatable {
        if(player.isOnGround) {
            if (BlockPos(player.pos).getBlock() == Blocks.LADDER) {
                player.velocity.y = 1.5
            }
        }
    }
}
