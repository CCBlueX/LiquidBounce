package net.ccbluex.liquidbounce.register

/**
 * Registers a command as an inbuilt command.
 * All annotated objects must have a `createCommand()` method that returns the command.
 *
 * Only in development versions when [dev] is true.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class IncludeCommand(val dev: Boolean = false)
