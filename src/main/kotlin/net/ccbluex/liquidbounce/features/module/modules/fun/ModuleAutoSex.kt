@file:Suppress("unused","all")

package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.texture.NativeImage
import net.minecraft.entity.player.PlayerEntity
import java.awt.Color
import java.net.URI
import java.util.*
import kotlin.random.Random

object ModuleAutoSex : ClientModule("AutoSex", Category.FUN) {

    private val targetRange by float("TargetRange", 5f, 1f..10f)
    private val mode by enumChoice("SexMode", SexMode.Active)
    private val msgDelay by int("MessageDelay", 1, 0..50, "seconds")

    private enum class SexMode(
        override val choiceName: String,
    ) : NamedChoice {
        Active("Active"),
        Passive("Passive"),
    }

    private val PASSIVE_MESSAGES = arrayOf(
        "It's so Biiiiiiig",
        "Be careful daddy <3",
        "Oh, I feel it inside me!"
    )
    private val ACTIVE_MESSAGES = arrayOf(
        "Oh, I'm cumming!",
        "Oh, ur pussy is so nice!",
        "Yeah, yeah",
        "I feel u!",
        "Oh, im inside u"
    )

    private var target: PlayerEntity? = null
    private var lastMessageTime = 0L
    private var lastSneakToggleTime = 0L

    private val movementInputHandler = handler<MovementInputEvent> { event ->
        when (mode) {
            SexMode.Active -> {
                if (System.currentTimeMillis() - lastSneakToggleTime > Random.nextLong(200, 1200)) {
                    event.sneak = !event.sneak
                    lastSneakToggleTime = System.currentTimeMillis()
                }
            }
            SexMode.Passive -> event.sneak = true
        }
    }

    private fun getSkinGrayscale(image: NativeImage): Float {
        var totalBrightness = 0f
        var pixelCount = 0
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val color = Color(image.getColorArgb(x, y))
                val brightness = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f)
                totalBrightness += brightness
                pixelCount++
            }
        }
        return totalBrightness / pixelCount / 255f
    }


    fun loadSkinImage(url: String): NativeImage? {
        return try {
            URI(url).toURL().openStream().use { NativeImage.read(it) }
        } catch (e: Exception) {
            null
        }
    }
    private val skinCache = mutableMapOf<UUID, NativeImage>()

    private fun estimateNiggerFromSkin(image: NativeImage): Gender? {
        var warmCount = 0
        var coolCount = 0
        var neutralCount = 0
        var pixelCount = 0

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val color = Color(image.getColorArgb(x, y))
                val r = color.red
                val g = color.green
                val b = color.blue
                if (color.alpha < 50) continue

                if ((r > 200 && g < 50 && b < 50) || (b > 200 && r < 50 && g < 50)) {
                    neutralCount++
                    continue
                }

                if (r > g && r > b && r > 80) warmCount++
                else if (b > r && b > g && b > 80) coolCount++
                else neutralCount++

                pixelCount++
            }
        }

        if (pixelCount == 0) return null

        val warmRatio = warmCount.toFloat() / pixelCount
        val coolRatio = coolCount.toFloat() / pixelCount

        return when {
            warmRatio > 0.25f && warmRatio > coolRatio * 1.5f -> Gender.FEMALE
            coolRatio > 0.25f && coolRatio > warmRatio * 1.5f -> Gender.MALE
            else -> null
        }
    }

    private enum class Gender { MALE, FEMALE }

    private fun getNearestPlayer(range: Float): PlayerEntity? {
        return mc.world?.players
            ?.filter { it != mc.player && it.isAlive && it.distanceTo(mc.player) <= range }
            ?.filter { player ->
                if (player is AbstractClientPlayerEntity) {
                    val uuid = player.uuid
                    val skinImage = skinCache[uuid] ?: run {
                        player.skinTextures?.textureUrl?.let { url ->
                            loadSkinImage(url)?.also { skinCache[uuid] = it }
                        }
                    }
                    skinImage?.let {
                        val gender = estimateNiggerFromSkin(it)
                        gender == Gender.FEMALE
                    } ?: false
                } else false
            }
            ?.minByOrNull { it.distanceTo(mc.player) }
    }


    private val tickHandler = tickHandler {
        if (player.isSpectator || player.isCreative) return@tickHandler

        target = if (target == null || target!!.squaredDistanceTo(player) > targetRange * targetRange) {
            getNearestPlayer(targetRange)
        } else {
            target
        }

        target ?: return@tickHandler

        if (System.currentTimeMillis() - lastMessageTime >= msgDelay * 1000L) {
            val messages = if (mode == SexMode.Active) ACTIVE_MESSAGES else PASSIVE_MESSAGES
            val message = messages.random()
            network.sendChatCommand("msg ${target!!.name.literalString} $message")
            lastMessageTime = System.currentTimeMillis()
        }
    }
}
