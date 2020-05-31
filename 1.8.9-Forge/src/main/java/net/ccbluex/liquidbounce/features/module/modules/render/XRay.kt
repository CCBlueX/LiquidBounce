/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.block.Block
import net.minecraft.init.Blocks

@ModuleInfo(name = "XRay", description = "Allows you to see ores through walls.", category = ModuleCategory.RENDER)
class XRay : Module() {
    val xrayBlocks = mutableListOf<Block>(
            Blocks.coal_ore,
            Blocks.iron_ore,
            Blocks.gold_ore,
            Blocks.redstone_ore,
            Blocks.lapis_ore,
            Blocks.diamond_ore,
            Blocks.emerald_ore,
            Blocks.quartz_ore,
            Blocks.clay,
            Blocks.glowstone,
            Blocks.crafting_table,
            Blocks.torch,
            Blocks.ladder,
            Blocks.tnt,
            Blocks.coal_block,
            Blocks.iron_block,
            Blocks.gold_block,
            Blocks.diamond_block,
            Blocks.emerald_block,
            Blocks.redstone_block,
            Blocks.lapis_block,
            Blocks.fire,
            Blocks.mossy_cobblestone,
            Blocks.mob_spawner,
            Blocks.end_portal_frame,
            Blocks.enchanting_table,
            Blocks.bookshelf,
            Blocks.command_block,
            Blocks.lava,
            Blocks.flowing_lava,
            Blocks.water,
            Blocks.flowing_water,
            Blocks.furnace,
            Blocks.lit_furnace
    )

    init {
        LiquidBounce.commandManager.registerCommand(object : Command("xray", emptyArray()) {

            override fun execute(args: Array<String>) {
                if (args.size > 1) {
                    if (args[1].equals("add", ignoreCase = true)) {
                        if (args.size > 2) {
                            try {
                                val block = try {
                                    Block.getBlockById(args[2].toInt())
                                } catch (exception: NumberFormatException) {
                                    val tmpBlock = Block.getBlockFromName(args[2])

                                    if (Block.getIdFromBlock(tmpBlock) <= 0 || tmpBlock == null) {
                                        chat("§7Block §8${args[2]}§7 does not exist!")
                                        return
                                    }

                                    tmpBlock
                                }

                                if (xrayBlocks.contains(block)) {
                                    chat("This block is already on the list.")
                                    return
                                }

                                xrayBlocks.add(block)
                                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.xrayConfig)
                                chat("§7Added block §8${block.localizedName}§7.")
                                playEdit()
                            } catch (exception: NumberFormatException) {
                                chatSyntaxError()
                            }

                            return
                        }

                        chatSyntax("xray add <block_id>")
                        return
                    }

                    if (args[1].equals("remove", ignoreCase = true)) {
                        if (args.size > 2) {
                            try {
                                var block: Block

                                try {
                                    block = Block.getBlockById(args[2].toInt())
                                } catch (exception: NumberFormatException) {
                                    block = Block.getBlockFromName(args[2])

                                    if (Block.getIdFromBlock(block) <= 0) {
                                        chat("§7Block §8${args[2]}§7 does not exist!")
                                        return
                                    }
                                }

                                if (!xrayBlocks.contains(block)) {
                                    chat("This block is not on the list.")
                                    return
                                }

                                xrayBlocks.remove(block)
                                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.xrayConfig)
                                chat("§7Removed block §8${block.localizedName}§7.")
                                playEdit()
                            } catch (exception: NumberFormatException) {
                                chatSyntaxError()
                            }

                            return
                        }
                        chatSyntax("xray remove <block_id>")
                        return
                    }

                    if (args[1].equals("list", ignoreCase = true)) {
                        chat("§8Xray blocks:")
                        xrayBlocks.forEach { chat("§8${it.localizedName} §7-§c ${Block.getIdFromBlock(it)}") }
                        return
                    }
                }

                chatSyntax("xray <add, remove, list>")
            }
        })
    }

    override fun onToggle(state: Boolean) {
        mc.renderGlobal.loadRenderers()
    }
}
