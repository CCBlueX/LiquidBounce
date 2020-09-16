/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "XRay", description = "Allows you to see ores through walls.", category = ModuleCategory.RENDER)
class XRay : Module() {

    val xrayBlocks = mutableListOf<IBlock>(
            classProvider.getBlockEnum(BlockType.COAL_ORE),
            classProvider.getBlockEnum(BlockType.IRON_ORE),
            classProvider.getBlockEnum(BlockType.GOLD_ORE),
            classProvider.getBlockEnum(BlockType.REDSTONE_ORE),
            classProvider.getBlockEnum(BlockType.LAPIS_ORE),
            classProvider.getBlockEnum(BlockType.DIAMOND_ORE),
            classProvider.getBlockEnum(BlockType.EMERALD_ORE),
            classProvider.getBlockEnum(BlockType.QUARTZ_ORE),
            classProvider.getBlockEnum(BlockType.CLAY),
            classProvider.getBlockEnum(BlockType.GLOWSTONE),
            classProvider.getBlockEnum(BlockType.CRAFTING_TABLE),
            classProvider.getBlockEnum(BlockType.TORCH),
            classProvider.getBlockEnum(BlockType.LADDER),
            classProvider.getBlockEnum(BlockType.TNT),
            classProvider.getBlockEnum(BlockType.COAL_BLOCK),
            classProvider.getBlockEnum(BlockType.IRON_BLOCK),
            classProvider.getBlockEnum(BlockType.GOLD_BLOCK),
            classProvider.getBlockEnum(BlockType.DIAMOND_BLOCK),
            classProvider.getBlockEnum(BlockType.EMERALD_BLOCK),
            classProvider.getBlockEnum(BlockType.REDSTONE_BLOCK),
            classProvider.getBlockEnum(BlockType.LAPIS_BLOCK),
            classProvider.getBlockEnum(BlockType.FIRE),
            classProvider.getBlockEnum(BlockType.MOSSY_COBBLESTONE),
            classProvider.getBlockEnum(BlockType.MOB_SPAWNER),
            classProvider.getBlockEnum(BlockType.END_PORTAL_FRAME),
            classProvider.getBlockEnum(BlockType.ENCHANTING_TABLE),
            classProvider.getBlockEnum(BlockType.BOOKSHELF),
            classProvider.getBlockEnum(BlockType.COMMAND_BLOCK),
            classProvider.getBlockEnum(BlockType.LAVA),
            classProvider.getBlockEnum(BlockType.FLOWING_LAVA),
            classProvider.getBlockEnum(BlockType.WATER),
            classProvider.getBlockEnum(BlockType.FLOWING_WATER),
            classProvider.getBlockEnum(BlockType.FURNACE),
            classProvider.getBlockEnum(BlockType.LIT_FURNACE)
    )

    override fun onToggle(state: Boolean) {
        mc.renderGlobal.loadRenderers()
    }
}
