/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils

class RenameCommand : Command("rename") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            if (mc.playerController.isNotCreative) {
                chat("\u00A7c\u00A7lError: \u00A73You need to be in creative mode.")
                return
            }

            val item = mc.thePlayer!!.heldItem

            if (item?.item == null) {
                chat("\u00A7c\u00A7lError: \u00A73You need to hold a item.")
                return
            }

            item.setStackDisplayName(ColorUtils.translateAlternateColorCodes(StringUtils.toCompleteString(args, 1)))
            mc.netHandler.addToSendQueue(classProvider.createCPacketCreativeInventoryAction(36 + mc.thePlayer!!.inventory.currentItem, item))
            chat("\u00A73Item renamed to '${item.displayName}\u00A73'")
            return
        }

        chatSyntax("rename <name>")
    }
}
