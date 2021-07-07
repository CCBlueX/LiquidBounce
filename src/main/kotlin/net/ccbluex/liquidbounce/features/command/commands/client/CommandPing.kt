package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

object CommandPing {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("ping")
            .handler { command, _ ->
                val ping = mc.networkHandler!!.getPlayerListEntry(mc.player!!.uuid)!!.latency
                chat(regular(command.result("pingCheck", variable(ping.toString()))))
            }
            .build()
    }
}
