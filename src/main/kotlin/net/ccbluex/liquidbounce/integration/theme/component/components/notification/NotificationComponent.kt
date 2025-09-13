package net.ccbluex.liquidbounce.integration.theme.component.components.notification

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent.Severity.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.notification.mode.NovolineMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment
import java.io.InputStream
import java.util.logging.Logger
import javax.sound.sampled.AudioSystem

object NotificationComponent: NativeComponent(
    "Notification", true, Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.RIGHT,
        horizontalOffset = 15,
        verticalAlignment = Alignment.ScreenAxisY.BOTTOM,
        verticalOffset = 30,
    )
)  {
    init {
        registerComponentListen(this)
    }

    val modes = choices(this, "Mode", NovolineMode, arrayOf(NovolineMode))
    val size by float("Size", 1f, 0.5f..1f)
    val backgroundColor by color("Background", Color4b.BLACK.withAlpha(75))
    fun playSound(resourcePath: String) {
        try {
            val inputStream: InputStream = javaClass.classLoader.getResourceAsStream(
                "resources/liquidbounce/$resourcePath")
                ?: return
            AudioSystem.getAudioInputStream(inputStream).use { audioStream ->
                val clip = AudioSystem.getClip()
                clip.open(audioStream)
                clip.framePosition = 0
                clip.start()
            }
        }catch (e: Exception) {
            Logger.getLogger("LiquidBounce").warning("Failed to play sound: ${e.message}")
        }

    }
    fun playSuccess() = playSound("sound/notification/success.wav")
    fun playEnabled() = playSound("sound/notification/enable.wav")
    fun playDisable() = playSound("sound/notification/disable.wav")
    fun playError() = playSound("sound/notification/error.wav")
    fun playInfo() = playSound("sound/notification/info.wav")
    fun playBlink() = playSound("sound/notification/blink.wav")
    fun playBlinked() = playSound("sound/notification/blinked.wav")

    @Suppress("unused")
    private val handleNotificationEvent = handler<NotificationEvent> { event ->
        when (event.severity) {
            SUCCESS -> playSuccess()
            ENABLED -> playEnabled()
            DISABLED -> playDisable()
            ERROR -> playError()
            INFO -> playInfo()
            BLINK -> playBlink()
            BLINKED -> playBlinked()
            BLINKING -> return@handler
        }
    }
    override fun onEnabled() {
        modes.activeChoice.enable()
    }

    override fun onDisabled() {
        modes.activeChoice.disable()
    }

}
