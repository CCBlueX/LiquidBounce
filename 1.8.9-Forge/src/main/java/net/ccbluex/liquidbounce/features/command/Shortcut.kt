package net.ccbluex.liquidbounce.features.command

class Shortcut(val name: String, val script: List<Pair<Command, Array<String>>>): Command(name, arrayOf()) {

    override fun execute(args: Array<String>) {
        script.forEach { it.first.execute(it.second) }
    }

}
