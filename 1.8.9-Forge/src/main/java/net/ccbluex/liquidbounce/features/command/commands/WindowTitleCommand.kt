package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import org.lwjgl.opengl.Display

class WindowTitleCommand : Command("windowtitle", emptyArray()) {
    // The title should be set when the client initializes
    private val originalTitle: String = Display.getTitle()

    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            val title = args.filterIndexed { index, _ -> index > 0 }.joinToString(separator = " ")

            Display.setTitle(title)

            chat("New Title: §a§l$title")
        } else {
            Display.setTitle(originalTitle)

            chat("Title was reset.")
        }


    }

}