/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.minecraft.enchantment.Enchantment
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction

class EnchantCommand : Command("enchant")
{
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler

        if (args.size > 2)
        {
            if (mc.playerController.isNotCreative)
            {
                chat(thePlayer, "\u00A7c\u00A7lError: \u00A73You need to be in creative mode.")
                return
            }

            val item = thePlayer.heldItem

            if (item?.item == null)
            {
                chat(thePlayer, "\u00A7c\u00A7lError: \u00A73You need to hold an item.")
                return
            }

            val enchantment = try
            {
                Enchantment.getEnchantmentById(args[1].toInt()) ?: run {
                    chat(thePlayer, "There is no enchantment with the ID '${args[1]}'")
                    return@execute
                }
            }
            catch (e: NumberFormatException)
            {
                Enchantment.getEnchantmentByLocation(args[1]) ?: run {
                    chat(thePlayer, "There is no enchantment with the name '${args[1]}'")
                    return@execute
                }
            }

            val level = try
            {
                args[2].toInt()
            }
            catch (e: NumberFormatException)
            {
                chatSyntaxError(thePlayer)
                return
            }

            item.addEnchantment(enchantment, level)
            netHandler.addToSendQueue(C10PacketCreativeInventoryAction(36 + thePlayer.inventory.currentItem, item))
            chat(thePlayer, "${enchantment.getTranslatedName(level)} added to ${item.displayName}.")
            return
        }

        chatSyntax(thePlayer, "enchant <type> <level>")
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        val prefix = args[0]
        return when (args.size)
        {
            1 -> return Enchantment.func_181077_c().map { it.resourcePath.lowercase() }.filter { it.startsWith(prefix, true) }.toList()
            else -> emptyList()
        }
    }
}
