package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.text.DecimalFormat

object CommandVClip {

    private val decimalFormat = DecimalFormat("##0.000")

    fun createCommand(): Command {
        return CommandBuilder
            .begin("vclip")
            .parameter(
                ParameterBuilder
                    .begin<Float>("distance")
                    .required()
                    .build()
            )
            .handler { command, args ->
                val y = (args[0] as String).toDoubleOrNull()
                val player = mc.player ?: throw CommandException(command.result("notInGame"))

                if (y == null) {
                    throw CommandException(command.result("invalidDistance"))
                }

                player.updatePosition(player.x, player.y + y, player.z)
                chat(regular(command.result("positionUpdated",
                                variable(decimalFormat.format(player.x)),
                                variable(decimalFormat.format(player.y)),
                                variable(decimalFormat.format(player.z)))))
            }
            .build()
    }
}
