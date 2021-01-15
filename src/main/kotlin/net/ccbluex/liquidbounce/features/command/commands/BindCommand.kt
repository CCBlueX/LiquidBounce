package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat
import org.lwjgl.glfw.GLFW

object BindCommand {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("bind")
            .description("Allows you to set keybinds")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .description("The name of the module")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            ).parameter(
                ParameterBuilder
                    .begin<String>("key")
                    .description("The new key to set the module")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler { args ->
                val name = args[0] as String
                val module = LiquidBounce.moduleManager.find { it.name.equals(name, true) } ?: return@handler false

                if (module == null) {
                    chat("Module §a§l" + args[1] + "§3 not found.")
                }

                try {
                    module.bind =
                        GLFW::class.java.getField("GLFW_KEY_" + (args[1] as String).toUpperCase()).getInt(GLFW::class);
                } catch(e: Exception) {
                    chat("invalid keybinding.");
                e.printStackTrace();
                }
                chat("set bind to " + (args[1] as String).toUpperCase())
                true
            }
            .build()
    }
}
