/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils

@ModuleInfo(name = "XRay", description = "Allows you to see ores through walls.", category = ModuleCategory.RENDER)
class XRay : Module()
{
	var orbfuscatorBypass = false

	val xrayBlocks = run {
		val provider = classProvider
		mutableSetOf(provider.getBlockEnum(BlockType.COAL_ORE), provider.getBlockEnum(BlockType.IRON_ORE), provider.getBlockEnum(BlockType.GOLD_ORE), provider.getBlockEnum(BlockType.REDSTONE_ORE), provider.getBlockEnum(BlockType.LAPIS_ORE), provider.getBlockEnum(BlockType.DIAMOND_ORE), provider.getBlockEnum(BlockType.EMERALD_ORE), provider.getBlockEnum(BlockType.QUARTZ_ORE), provider.getBlockEnum(BlockType.CLAY), provider.getBlockEnum(BlockType.GLOWSTONE), provider.getBlockEnum(BlockType.CRAFTING_TABLE), provider.getBlockEnum(BlockType.TORCH), provider.getBlockEnum(BlockType.LADDER), provider.getBlockEnum(BlockType.TNT), provider.getBlockEnum(BlockType.COAL_BLOCK), provider.getBlockEnum(BlockType.IRON_BLOCK), provider.getBlockEnum(BlockType.GOLD_BLOCK), provider.getBlockEnum(BlockType.DIAMOND_BLOCK), provider.getBlockEnum(BlockType.EMERALD_BLOCK), provider.getBlockEnum(BlockType.REDSTONE_BLOCK), provider.getBlockEnum(BlockType.LAPIS_BLOCK), provider.getBlockEnum(BlockType.FIRE), provider.getBlockEnum(BlockType.MOSSY_COBBLESTONE), provider.getBlockEnum(BlockType.MOB_SPAWNER), provider.getBlockEnum(BlockType.END_PORTAL_FRAME), provider.getBlockEnum(BlockType.ENCHANTING_TABLE), provider.getBlockEnum(BlockType.BOOKSHELF), provider.getBlockEnum(BlockType.COMMAND_BLOCK), provider.getBlockEnum(BlockType.LAVA), provider.getBlockEnum(BlockType.FLOWING_LAVA), provider.getBlockEnum(BlockType.WATER), provider.getBlockEnum(BlockType.FLOWING_WATER), provider.getBlockEnum(BlockType.FURNACE), provider.getBlockEnum(BlockType.LIT_FURNACE))
	}

	fun canBeRendered(block: IBlock, pos: WBlockPos?): Boolean
	{
		val theWorld = mc.theWorld ?: return false

		return xrayBlocks.contains(block) && (pos == null || !orbfuscatorBypass || !BlockUtils.getBlock(theWorld, pos).isOpaqueCube)
	}

	override fun onToggle(state: Boolean)
	{
		mc.renderGlobal.loadRenderers()
	}

	override val tag: String?
		get() = if (orbfuscatorBypass) "Orbfuscator" else null
}
