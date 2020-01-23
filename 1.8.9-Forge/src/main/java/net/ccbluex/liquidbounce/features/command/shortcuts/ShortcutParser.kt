/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.shortcuts

object ShortcutParser {
    private val SEPARATOR = ";".codePointAt(0)

    fun parse(script: String): List<List<String>> {
        val tokens = tokenize(script)

        val parsed = mutableListOf<List<String>>()
        val tmpStatement = mutableListOf<String>()

        for (token in tokens) {
            when (token) {
                is Literal -> tmpStatement += token.literal
                is StatementEnd -> {
                    parsed += tmpStatement.toList()

                    tmpStatement.clear()
                }
            }
        }

        if (tmpStatement.isNotEmpty())
            throw IllegalArgumentException("Unexpected end of statement!")

        return parsed
    }

    private fun tokenize(script: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val tokenBuf = StringBuilder()

        for (code in script.codePoints()) {
            when {
                Character.isWhitespace(code) -> finishLiteral(tokens, tokenBuf)
                code == SEPARATOR -> {
                    finishLiteral(tokens, tokenBuf)

                    tokens += StatementEnd()
                }
                else -> tokenBuf.appendCodePoint(code)
            }
        }

        if (tokenBuf.isNotEmpty())
            throw IllegalArgumentException("Unexpected end of literal!")

        return tokens
    }

    private fun finishLiteral(tokens: MutableList<Token>, tokenBuf: StringBuilder) {
        if (tokenBuf.isNotEmpty()) {
            tokens += Literal(tokenBuf.toString())

            tokenBuf.clear()
        }
    }
}
