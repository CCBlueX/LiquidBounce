package net.ccbluex.liquidbounce.features.command.commands.module

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoAccount

/**
 * AutoAccount Command
 *
 * Allows you to manually trigger the actions of [ModuleAutoAccount].
 *
 * Module: [ModuleAutoAccount]
 */
object CommandAutoAccount : Command.Factory {

    @Suppress("SpellCheckingInspection")
    override fun createCommand(): Command {
        return CommandBuilder
            .begin("autoaccount")
            .requiresIngame()
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("register")
                    .handler {
                        ModuleAutoAccount.register()
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("login")
                    .handler {
                        ModuleAutoAccount.login()
                    }
                    .build()
            )
            .build()
    }

}
