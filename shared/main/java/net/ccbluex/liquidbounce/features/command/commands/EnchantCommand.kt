/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

class EnchantCommand : Command("enchant") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 2) {
            if (mc.playerController.isNotCreative) {
                chat("§c§lError: §3You need to be in creative mode.")
                return
            }

            val item = mc.thePlayer?.heldItem

            if (item?.item == null) {
                chat("§c§lError: §3You need to hold an item.")
                return
            }

            val enchantID: Int = try {
                args[1].toInt()
            } catch (e: NumberFormatException) {
                val enchantment = functions.getEnchantmentByLocation(args[1])

                if (enchantment == null) {
                    chat("There is no enchantment with the name '${args[1]}'")
                    return
                }

                enchantment.effectId
            }

            val enchantment = functions.getEnchantmentById(enchantID)

            if (enchantment == null) {
                chat("There is no enchantment with the ID '$enchantID'")
                return
            }

            val level = try {
                args[2].toInt()
            } catch (e: NumberFormatException) {
                chatSyntaxError()
                return
            }

            item.addEnchantment(enchantment, level)
            mc.netHandler.addToSendQueue(classProvider.createCPacketCreativeInventoryAction(36 + mc.thePlayer!!.inventory.currentItem, item))
            chat("${enchantment.getTranslatedName(level)} added to ${item.displayName}.")
            return
        }
        chatSyntax("enchant <type> [level]")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> {
                return functions.getEnchantments()
                        .map { it.resourcePath.toLowerCase() }
                        .filter { it.startsWith(args[0], true) }
            }
            else -> emptyList()
        }
    }
}