package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.render.XRay
import net.minecraft.block.Block

class XrayCommand : Command("xray") {

    val xRay = LiquidBounce.moduleManager.getModule(XRay::class.java) as XRay

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            if (args[1].equals("add", ignoreCase = true)) {
                if (args.size > 2) {
                    try {
                        val block = try {
                            functions.getBlockById(args[2].toInt())
                        } catch (exception: NumberFormatException) {
                            val tmpBlock = functions.getBlockFromName(args[2])

                            if (tmpBlock == null || functions.getIdFromBlock(tmpBlock) <= 0) {
                                chat("§7Block §8${args[2]}§7 does not exist!")
                                return
                            }

                            tmpBlock
                        }

                        if (block == null || xRay.xrayBlocks.contains(block)) {
                            chat("This block is already on the list.")
                            return
                        }

                        xRay.xrayBlocks.add(block)
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
                        val block = try {
                            functions.getBlockById(args[2].toInt())
                        } catch (exception: NumberFormatException) {
                            val tmpBlock = functions.getBlockFromName(args[2])

                            if (tmpBlock == null || functions.getIdFromBlock(tmpBlock) <= 0) {
                                chat("§7Block §8${args[2]}§7 does not exist!")
                                return
                            }

                            tmpBlock
                        }

                        if (block == null || !xRay.xrayBlocks.contains(block)) {
                            chat("This block is not on the list.")
                            return
                        }

                        xRay.xrayBlocks.remove(block)
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
                xRay.xrayBlocks.forEach { chat("§8${it.localizedName} §7-§c ${functions.getIdFromBlock(it)}") }
                return
            }
        }
        chatSyntax("xray <add, remove, list>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty())
            return emptyList()

        return when (args.size) {
            1 -> {
                arrayOf("add", "remove", "list")
                        .map { it.toLowerCase() }
                        .filter { it.startsWith(args[0], true) }
            }
            2 -> {
                when (args[0].toLowerCase()) {
                    "add" -> {
                        return functions.getBlockRegistryKeys()
                                .map { it.resourcePath.toLowerCase() }
                                .filter { Block.getBlockFromName(it.toLowerCase()) != null }
                                .filter { !xRay.xrayBlocks.contains(functions.getBlockFromName(it.toLowerCase())) }
                                .filter { it.startsWith(args[1], true) }
                    }
                    "remove" -> {
                        return functions.getBlockRegistryKeys()
                                .map { it.resourcePath.toLowerCase() }
                                .filter { xRay.xrayBlocks.contains(functions.getBlockFromName(it)) }
                                .filter { it.startsWith(args[1], true) }
                    }
                    else -> emptyList()
                }

            }
            else -> emptyList()
        }
    }
}