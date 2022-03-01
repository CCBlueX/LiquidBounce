package net.ccbluex.liquidbounce.features.command.commands.creative

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.enchantment.Enchantments
import net.minecraft.util.Hand
import net.minecraft.util.registry.Registry

object CommandEnchant {
    fun createCommand(): Command{
        return CommandBuilder
            .begin("enchant")
            .handler { command, _ ->
                if (mc.interactionManager?.hasCreativeInventory() == false) {
                    throw CommandException(command.result("mustBeCreative"))
                }

                val itemStack = mc.player?.getStackInHand(Hand.MAIN_HAND)
                if (itemStack.isNothing()) {
                    throw CommandException(command.result("mustHoldItem"))
                }

                for (enchantment in Registry.ENCHANTMENT) {
                    if (enchantment == Enchantments.SILK_TOUCH)
                        continue
                    if (enchantment.isCursed)
                        continue
                    if (enchantment == Enchantments.QUICK_CHARGE) {
                        itemStack?.addEnchantment(enchantment, 5)
                        continue
                    }
                    if (enchantment == Enchantments.LURE) {
                        itemStack?.addEnchantment(enchantment, 5);
                        continue;
                    }
                    if(enchantment == Enchantments.LUCK_OF_THE_SEA) {
                        itemStack?.addEnchantment(enchantment, 84);
                        continue;
                    }
                    itemStack?.addEnchantment(enchantment, 127)
                }
            }
            .build()
    }
}
