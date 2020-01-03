package net.ccbluex.liquidbounce.features.command.shortcuts

open class Token

class Literal(val literal: String): Token() {
    override fun toString(): String {
        return "Literal($literal)"
    }
}

class StatementEnd: Token() {
    override fun toString(): String {
        return "StatementEnd"
    }
}
