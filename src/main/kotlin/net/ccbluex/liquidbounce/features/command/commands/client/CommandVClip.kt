package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.blockParameter
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.client.MinecraftClient
object CommandVClip {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("vclip")
            .parameter(
                blockParameter("distance")
                    .required()
                    .build()
            )
            .handler { command, args ->
                val y = args[0] as String
                val player = MinecraftClient.getInstance().player

                if (player != null) {
                    player.updatePosition(player.x, player.y + y.toDouble(), player.z)
                    chat(
                        regular(
                            command.result(
                                "positionUpdated",
                                variable(player.x.toString()),
                                variable(player.y.toString()),
                                variable(player.z.toString())
                            )
                        )
                    )
                } else {
                    throw CommandException(command.result("playerNotFound"))
                }
            }
            .build()
    }
}
