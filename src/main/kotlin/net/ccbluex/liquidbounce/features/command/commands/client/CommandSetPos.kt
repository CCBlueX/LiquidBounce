package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import java.text.DecimalFormat

object CommandSetPos : QuickImports {

    private val decimalFormat = DecimalFormat("##0.000")

    fun createCommand(): Command {
        return CommandBuilder
            .begin("setpos")
            .parameter(
                ParameterBuilder
                    .begin<Double>("x")
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<Double>("y")
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<Double>("z")
                    .required()
                    .build()
            )
            .handler { command, args ->
                val x = (args[0] as String).toDoubleOrNull() ?: throw CommandException(command.result("invalidX"))
                val y = (args[1] as String).toDoubleOrNull() ?: throw CommandException(command.result("invalidY"))
                val z = (args[2] as String).toDoubleOrNull() ?: throw CommandException(command.result("invalidZ"))


                network.sendPacket(MovePacketType.POSITION_AND_ON_GROUND.generatePacket().apply {
                    this.x = x
                    this.y = y
                    this.z = z
                })


                player.updatePosition(x, y, z)


                chat(
                    regular(
                        command.result(
                            "positionUpdated",
                            variable(decimalFormat.format(player.x)),
                            variable(decimalFormat.format(player.y)),
                            variable(decimalFormat.format(player.z))
                        )
                    )
                )
            }
            .build()
    }
}
