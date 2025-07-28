package net.ccbluex.liquidbounce.features.command

/**
 * Provides a [Command]c instance.
 *
 * Add annotation [net.ccbluex.liquidbounce.annotations.InbuiltCommandFactory] to auto-register it in [CommandManager].
 *
 * @see net.ccbluex.liquidbounce.annotations.InbuiltCommandFactory
 */
fun interface CommandFactory {

    /**
     * Creates the [Command] and is run only once by the [CommandManager].
     */
    fun createCommand(): Command

}
