/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class HoloStandCommand : Command("holostand") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 4) {
            if (mc.playerController.isNotCreative) {
                chat("§c§lError: §3You need to be in creative mode.")
                return
            }

            try {
                val x = args[1].toDouble()
                val y = args[2].toDouble()
                val z = args[3].toDouble()
                val message = StringUtils.toCompleteString(args, 4)

                val itemStack = classProvider.createItemStack(classProvider.getItemEnum(ItemType.ARMOR_STAND))
                val base = classProvider.createNBTTagCompound()
                val entityTag = classProvider.createNBTTagCompound()
                entityTag.setInteger("Invisible", 1)
                entityTag.setString("CustomName", message)
                entityTag.setInteger("CustomNameVisible", 1)
                entityTag.setInteger("NoGravity", 1)
                val position = classProvider.createNBTTagList()
                position.appendTag(classProvider.createNBTTagDouble(x))
                position.appendTag(classProvider.createNBTTagDouble(y))
                position.appendTag(classProvider.createNBTTagDouble(z))
                entityTag.setTag("Pos", position)
                base.setTag("EntityTag", entityTag)
                itemStack.tagCompound = base
                itemStack.setStackDisplayName("§c§lHolo§eStand")
                mc.netHandler.addToSendQueue(classProvider.createCPacketCreativeInventoryAction(36, itemStack))

                chat("The HoloStand was successfully added to your inventory.")
            } catch (exception: NumberFormatException) {
                chatSyntaxError()
            }

            return
        }

        chatSyntax("holostand <x> <y> <z> <message...>")
    }
}