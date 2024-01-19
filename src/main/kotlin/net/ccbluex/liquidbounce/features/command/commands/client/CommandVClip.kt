package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.blockParameter
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.resource.language.I18n.translate

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
                val y = (args[0] as String).toDoubleOrNull()
                val player = mc.player

                if (y == null) {
                    chat(regular(translate("liquidbounce.command.vclip.error.invalidDistance")))
                    return@handler
                }

                if (player != null) {
                    player.updatePosition(player.x, player.y + y, player.z)
                    chat(
                        regular(
                            command.result(
                                translate("liquidbounce.command.vclip.result.positionUpdated"),
                                variable(player.x.toString()),
                                variable(player.y.toString()),
                                variable(player.z.toString())
                            )
                        )
                    )
                } else {
                    throw CommandException(command.result(translate("liquidbounce.command.vclip.error.notInGame")))
                }
            }
            .build()
    }
}
