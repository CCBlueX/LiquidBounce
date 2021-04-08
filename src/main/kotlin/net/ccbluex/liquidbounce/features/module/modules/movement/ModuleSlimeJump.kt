package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Choice
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos

object ModuleSlimeJump : Module("SlimeJump", Category.MOVEMENT) {

    private val modes = choices("Mode", "Add") {
        Add
        Set
    }
    private val motion by float("Motion", 0.42f, 0.2f..1f)

    private object Add : Choice("Add", modes) {
        val repeatable = repeatable {
            if(BlockPos(player.pos).down().getBlock() == Blocks.SLIME_BLOCK && mc.options.keyJump.isPressed) {
                player.velocity.y += motion
            }
        }
    }
    private object Set : Choice("Set", modes) {
        val repeatable = repeatable {
            if(BlockPos(player.pos).down().getBlock() == Blocks.SLIME_BLOCK && mc.options.keyJump.isPressed) {
                player.velocity.y = motion.toDouble()
            }
        }
    }
}
