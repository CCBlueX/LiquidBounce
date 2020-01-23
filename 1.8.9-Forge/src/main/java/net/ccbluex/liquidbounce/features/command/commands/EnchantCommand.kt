package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.minecraft.enchantment.Enchantment

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class EnchantCommand : Command("enchant", emptyArray()) {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 2) {
            if (mc.playerController.isNotCreative) {
                chat("§c§lError: §3You need creative mode.")
                return
            }

            val item = mc.thePlayer.heldItem

            if (item == null || item.item == null) {
                chat("§c§lError: §3You need to hold a item.")
                return
            }

            val enchantID = try {
                args[1].toInt()
            } catch (e: NumberFormatException) {
                val enchantment = Enchantment.getEnchantmentByLocation(args[1])

                if (enchantment == null) {
                    chat("There is no such enchantment with the name " + args[1])
                    return
                }

                enchantment.effectId
            }

            val enchantment = Enchantment.getEnchantmentById(enchantID)
            if (enchantment == null) {
                chat("There is no such enchantment with ID $enchantID")
                return
            }

            val level = try {
                args[2].toInt()
            } catch (e: NumberFormatException) {
                chatSyntaxError()
                return
            }

            item.addEnchantment(enchantment, level)
            chat("${enchantment.getTranslatedName(level)} added to ${item.displayName}.")
            return
        }

        chatSyntax("enchant <type> [level]")
    }

}