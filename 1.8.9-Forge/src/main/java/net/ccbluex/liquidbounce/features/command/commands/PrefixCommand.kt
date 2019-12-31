package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command

class PrefixCommand : Command("prefix", emptyArray()) {

    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("prefix <character>")
            return
        }

        val prefix = args[1]

        if (prefix.length > 1) {
            chat("§cPrefix can only be one character long!")
            return
        }

        LiquidBounce.CLIENT.commandManager.prefix = prefix.single()
        LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.valuesConfig)

        chat("Successfully changed command prefix to '§8$prefix§3'")
    }
}