package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.render.XRay
import net.ccbluex.liquidbounce.file.FileManager
import net.minecraft.block.Block

class XrayCommand : Command("xray")
{

    val xRay = LiquidBounce.moduleManager[XRay::class.java] as XRay

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        if (args.size > 1)
        {
            when (args[1].lowercase())
            {
                "add" ->
                {
                    if (args.size > 2)
                    {
                        try
                        {
                            val block = try
                            {
                                Block.getBlockById(args[2].toInt())
                            }
                            catch (exception: NumberFormatException)
                            {
                                val tmpBlock = Block.getBlockFromName(args[2])

                                if (tmpBlock == null || Block.getIdFromBlock(tmpBlock) <= 0)
                                {
                                    chat(thePlayer, "\u00A77Block \u00A78${args[2]}\u00A77 does not exist!")
                                    return
                                }

                                tmpBlock
                            }

                            if (block == null || xRay.xrayBlocks.contains(block))
                            {
                                chat(thePlayer, "This block is already on the list.")
                                return
                            }

                            xRay.xrayBlocks.add(block)
                            if (xRay.state) mc.renderGlobal.loadRenderers()
                            FileManager.saveConfig(LiquidBounce.fileManager.xrayConfig)
                            chat(thePlayer, "\u00A77Added block \u00A78${block.localizedName}\u00A77.")
                            playEdit()
                        }
                        catch (exception: NumberFormatException)
                        {
                            chatSyntaxError(thePlayer)
                        }

                        return
                    }

                    chatSyntax(thePlayer, "xray add <block_id>")
                    return
                }

                "remove" ->
                {
                    if (args.size > 2)
                    {
                        try
                        {
                            val block = try
                            {
                                Block.getBlockById(args[2].toInt())
                            }
                            catch (exception: NumberFormatException)
                            {
                                val tmpBlock = Block.getBlockFromName(args[2])

                                if (tmpBlock == null || Block.getIdFromBlock(tmpBlock) <= 0)
                                {
                                    chat(thePlayer, "\u00A77Block \u00A78${args[2]}\u00A77 does not exist!")
                                    return
                                }

                                tmpBlock
                            }

                            if (block == null || !xRay.xrayBlocks.contains(block))
                            {
                                chat(thePlayer, "This block is not on the list.")
                                return
                            }

                            xRay.xrayBlocks.remove(block)
                            if (xRay.state) mc.renderGlobal.loadRenderers()
                            FileManager.saveConfig(LiquidBounce.fileManager.xrayConfig)
                            chat(thePlayer, "\u00A77Removed block \u00A78${block.localizedName}\u00A77.")
                            playEdit()
                        }
                        catch (exception: NumberFormatException)
                        {
                            chatSyntaxError(thePlayer)
                        }

                        return
                    }
                    chatSyntax(thePlayer, "xray remove <block_id>")
                    return
                }

                "list" ->
                {
                    chat(thePlayer, "\u00A78Xray blocks:")
                    xRay.xrayBlocks.forEach { chat(thePlayer, "\u00A78${it.localizedName} \u00A77-\u00A7c ${Block.getIdFromBlock(it)}") }
                    return
                }

                "orbfuscatorbypass" ->
                {
                    xRay.orbfuscatorBypass = !xRay.orbfuscatorBypass
                    if (xRay.state) mc.renderGlobal.loadRenderers()
                    FileManager.saveConfig(LiquidBounce.fileManager.xrayConfig)
                    chat(thePlayer, "\u00A78Xray orbfuscator bypass has been ${if (xRay.orbfuscatorBypass) "enabled" else "disabled"}")
                    playEdit()
                    return
                }
            }
        }

        chatSyntax(thePlayer, "xray <add, remove, list, orbfuscatorBypass>")
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        return when (args.size)
        {
            1 -> arrayOf("add", "remove", "list", "orbfuscatorBypass").filter { it.startsWith(args[0], ignoreCase = true) }

            2 ->
            {
                val blockRegistryKeys = Block.blockRegistry.keys
                val xrayBlocks = xRay.xrayBlocks

                when (args[0].lowercase())
                {
                    "add" -> return blockRegistryKeys.asSequence().map { it.resourcePath.lowercase() }.filter { !xrayBlocks.contains((Block.getBlockFromName(it) ?: return@filter false)) }.filter { it.startsWith(args[1], true) }.toList()
                    "remove" -> return blockRegistryKeys.asSequence().map { it.resourcePath.lowercase() }.filter { xrayBlocks.contains(Block.getBlockFromName(it) ?: return@filter false) }.filter { it.startsWith(args[1], true) }.toList()
                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }
}
