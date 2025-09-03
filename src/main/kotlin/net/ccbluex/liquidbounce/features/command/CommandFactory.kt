package net.ccbluex.liquidbounce.features.command

/**
 * Provides a [Command] to the [CommandManager].
 */
fun interface CommandFactory {

    /**
     * Creates the [Command] and is run only once by the [CommandManager].
     */
    fun createCommand(): Command

}
