package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.config.types.NamedChoice

data class ScriptDebugOptions(
    val enabled: Boolean = false,
    val protocol: DebugProtocol = DebugProtocol.INSPECT,
    val suspendOnStart: Boolean = false,
    val inspectInternals: Boolean = false,
    val port: Int = 4242
)

enum class DebugProtocol(override val choiceName: String) : NamedChoice {
    DAP("DAP"),
    INSPECT("INSPECT"),
}
