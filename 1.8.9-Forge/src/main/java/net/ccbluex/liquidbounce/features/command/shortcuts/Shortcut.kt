package net.ccbluex.liquidbounce.features.command.shortcuts

import net.ccbluex.liquidbounce.features.command.Command

class Shortcut(val name: String, val script: List<Pair<Command, Array<String>>>): Command(name, arrayOf()) {

    override fun execute(args: Array<String>) {
        script.forEach { it.first.execute(it.second) }
    }

}
