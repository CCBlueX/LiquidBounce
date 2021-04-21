package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos

object ModuleBlockBounce : Module("BlockBounce", Category.MOVEMENT) {

    private val modes = choices("Mode", "Add") {
        Add
        Set
    }
    private val motion by float("Motion", 0.42f, 0.2f..1f)

    val blockPos = BlockPos(player.pos).down().getBlock()
    private object Add : Choice("Add", modes) {
        val repeatable = repeatable {
            if (blockPos == Blocks.SLIME_BLOCK || blockPos == Blocks.HONEY_BLOCK || allBeds() && mc.options.keyJump.isPressed) {
                player.velocity.y += motion
            }
        }
    }
    private object Set : Choice("Set", modes) {
        val repeatable = repeatable {
            if (blockPos == Blocks.SLIME_BLOCK || blockPos == Blocks.HONEY_BLOCK || allBeds() && mc.options.keyJump.isPressed) {
                player.velocity.y = motion.toDouble()
            }
        }
    }

    // Perhaps not the best way but still works
    fun allBeds(): Boolean {
        return when(BlockPos(player.pos).down().getBlock()) {
            Blocks.RED_BED -> true
            Blocks.BLACK_BED -> true
            Blocks.BLUE_BED -> true
            Blocks.BROWN_BED -> true
            Blocks.CYAN_BED -> true
            Blocks.GRAY_BED -> true
            Blocks.GREEN_BED -> true
            Blocks.LIGHT_BLUE_BED -> true
            Blocks.LIGHT_GRAY_BED -> true
            Blocks.LIME_BED -> true
            Blocks.MAGENTA_BED -> true
            Blocks.ORANGE_BED -> true
            Blocks.PINK_BED -> true
            Blocks.PURPLE_BED -> true
            Blocks.WHITE_BED -> true
            Blocks.YELLOW_BED -> true
            else -> false
        }
    }
}
