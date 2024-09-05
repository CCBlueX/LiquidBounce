package net.ccbluex.liquidbounce.features.command.commands.utility

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoAccount
import net.ccbluex.liquidbounce.register.IncludeCommand

/**
 * AutoAccount Command
 *
 * Allows you to manually trigger the actions of [ModuleAutoAccount].
 */
@IncludeCommand
object CommandAutoAccount {

    @Suppress("SpellCheckingInspection")
    fun createCommand(): Command {
        return CommandBuilder
            .begin("autoaccount")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("register")
                    .handler {_, _ ->
                        ModuleAutoAccount.register()
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("login")
                    .handler {_, _ ->
                        ModuleAutoAccount.login()
                    }
                    .build()
            )
            .build()
    }

}
