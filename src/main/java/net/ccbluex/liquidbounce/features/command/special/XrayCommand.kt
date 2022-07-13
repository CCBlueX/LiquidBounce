package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.render.XRay
import net.ccbluex.liquidbounce.file.FileManager
import net.minecraft.block.Block
import net.minecraft.client.entity.EntityPlayerSP

class XrayCommand : Command("xray")
{
    private val xRay = LiquidBounce.moduleManager[XRay::class.java] as XRay

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
                        addBlock(thePlayer, args[2])
                        return
                    }

                    chatSyntax(thePlayer, "xray add <block_id>")
                    return
                }

                "remove" ->
                {
                    if (args.size > 2)
                    {
                        removeBlock(thePlayer, args[2])
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

    private fun addBlock(thePlayer: EntityPlayerSP?, blockName: String)
    {
        val block = parseBlockName(thePlayer, blockName) ?: return

        if (xRay.xrayBlocks.contains(block))
        {
            chat(thePlayer, "This block is already on the list.")
            return
        }

        xRay.xrayBlocks.add(block)

        // Refresh XRay
        if (xRay.state) mc.renderGlobal.loadRenderers()

        FileManager.saveConfig(LiquidBounce.fileManager.xrayConfig)
        chat(thePlayer, "\u00A77Added block \u00A78${block.localizedName}\u00A77.")
        playEdit()
    }

    private fun removeBlock(thePlayer: EntityPlayerSP?, blockName: String)
    {
        val block = parseBlockName(thePlayer, blockName) ?: return
        if (!xRay.xrayBlocks.contains(block))
        {
            chat(thePlayer, "This block is not on the list.")
            return
        }

        xRay.xrayBlocks.remove(block)

        // Refresh XRay
        if (xRay.state) mc.renderGlobal.loadRenderers()
        FileManager.saveConfig(LiquidBounce.fileManager.xrayConfig)
        chat(thePlayer, "\u00A77Removed block \u00A78${block.localizedName}\u00A77.")
        playEdit()
    }

    private fun parseBlockName(thePlayer: EntityPlayerSP?, blockName: String): Block? = blockName.toIntOrNull()?.let(Block::getBlockById) ?: run {
        val tmpBlock = Block.getBlockFromName(blockName)

        if (tmpBlock == null || Block.getIdFromBlock(tmpBlock) <= 0)
        {
            chat(thePlayer, "\u00A77Block \u00A78$blockName\u00A77 does not exist!")
            null
        }
        else tmpBlock
    }
}
