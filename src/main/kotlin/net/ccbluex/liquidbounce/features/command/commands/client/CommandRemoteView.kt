package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

object CommandRemoteView {

    var pName: String? = null

    fun createCommand(): Command {
        return CommandBuilder
            .begin("remoteview")
            .alias("rv")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("off")
                    .handler { command, _ ->
                        if (mc.getCameraEntity() != mc.player) {
                            mc.setCameraEntity(mc.player)
                            chat(regular(command.result("off", variable(pName.toString()))))
                            pName = null
                        } else {
                            chat(regular(command.result("alreadyOff")))
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("view")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .useMinecraftAutoCompletion()
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        for (entity in mc.world!!.entities) {
                            if (name.equals(entity.entityName, true)) {
                                if (mc.getCameraEntity() == entity) {
                                    chat(regular(command.result("alreadyViewing", variable(entity.entityName))))
                                    return@handler
                                }
                                mc.setCameraEntity(entity)
                                pName = entity.entityName
                                chat(regular(command.result("viewPlayer", variable(entity.entityName))))
                                chat(regular(command.result("caseOff", variable(entity.entityName))))
                            }
                        }
                    }
                    .build()
            ).build()
    }
}
