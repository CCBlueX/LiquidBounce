package net.ccbluex.liquidbounce.features.command.commands

import com.darkprograms.speech.translator.GoogleTranslate
import net.ccbluex.liquidbounce.features.command.Command
import java.util.*

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class TranslateCommand : Command("translate", emptyArray()) {
    companion object {
        val LANGUAGES = arrayOf("af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs", "bg", "ca", "ceb",
                "zh-CN", "zh", "zh-TW", "co", "hr", "cs", "da", "nl", "en", "eo", "et", "fi", "fr", "fy", "gl", "ka", "de",
                "el", "gu", "ht", "ha", "haw", "he", "iw", "hi", "hmn", "hu", "is", "ig", "id", "ga", "it", "ja", "jw", "kn",
                "kk", "km", "ko", "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mn",
                "my", "ne", "no", "ny", "ps", "fa", "pl", "pt", "pa", "ro", "ru", "sm", "gd", "sr", "st", "sn", "sd", "si",
                "sk", "sl", "so", "es", "su", "sw", "sv", "tl", "tg", "ta", "te", "th", "tr", "uk", "ur", "uz", "vi", "cy",
                "xh", "yi", "yo", "zu")
    }

    /**
     * Execute command
     *
     * @param args arguments by user
     */
    override fun execute(args: Array<String>) {
        if (args.size <= 3) {
            chatSyntax("translate <language> <text>")
            return
        }

        val lang = args[1].toLowerCase(Locale.ENGLISH)

        if (!LANGUAGES.contains(lang)) {
            chat("§cLanguage '$lang' isn't supported. Here is a list with all available languages: https://cloud.google.com/translate/docs/languages")
            return
        }

        val sb = StringBuilder()

        for (i in 2 until args.size) {
            sb.append(args[i]).append(" ")
        }

        Thread {
            try {
                mc.thePlayer.sendChatMessage(GoogleTranslate.translate(lang, sb.substring(0, sb.length - 1)))
            } catch (e: Exception) {
                chat("§cTranslation error: $e")
            }
        }.start()

    }

}