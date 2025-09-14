package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.text.Text
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit

object GameWins: EventListener {
    private var titleField: Field? = null
    private var subtitleField: Field? = null
    private var lastWinDetectionTime = 0L
    private val cooldownMillis = TimeUnit.SECONDS.toMillis(10)

    var victoryCount = 0
        private set


    init {
        try {
            titleField = mc.inGameHud.javaClass.getDeclaredField("title").apply { isAccessible = true }
            subtitleField = mc.inGameHud.javaClass.getDeclaredField("subtitle").apply { isAccessible = true }
        } catch (e: Exception) {
            println("Failed to access title/subtitle fields: ${e.message}")
        }

        handler<GameTickEvent> { event ->
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastWinDetectionTime < cooldownMillis) {
                    return@handler
                }

                val currentTitle = getTitle()
                val currentSubtitle = getSubtitle()

                if (checkWinCondition(currentTitle) || checkWinCondition(currentSubtitle)) {
                    lastWinDetectionTime = currentTime
                    victoryCount++
                }
            } catch (e: Exception) {
                println("Error processing win condition: ${e.message}")
            }
        }

        handler<ChatReceiveEvent> { event ->
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastWinDetectionTime < cooldownMillis) {
                    return@handler
                }

                if (checkChatWinCondition(event.textData)) {
                    lastWinDetectionTime = currentTime
                    victoryCount++
                }
            } catch (e: Exception) {
                println("Error processing chat win condition: ${e.message}")
            }
        }
    }

    private inline fun <T> safeReflection(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            println("Reflection error: ${e.message}")
            null
        }
    }

    private fun getTitle(): Text? = safeReflection {
        titleField?.get(mc.inGameHud) as? Text
    }

    private fun getSubtitle(): Text? = safeReflection {
        subtitleField?.get(mc.inGameHud) as? Text
    }

    private fun checkWinCondition(text: Text?): Boolean {
        if (text == null || text.string.isEmpty()) return false
        return winKeywordsTitle.any { keyword ->
            text.string.contains(keyword, ignoreCase = true)
        }
    }

    private fun checkChatWinCondition(text: Text): Boolean {
        val message = text.string
        if (message.isEmpty()) return false
        return winKeywordsChatMessage.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }

    private val winKeywordsTitle = listOf(
        "victory", "you win", "Good Game",
        "恭喜你赢下了比赛","你是最后的站立者",
        "victoire", "gagné",
        "gewonnen",
        "vittoria",
        "¡ganaste!", "victoria",
        "победа",
        "승리",
        "勝った", "勝利"
    )

    private val winKeywordsChatMessage = listOf(
        "YOU WON!!!",
    )
}
