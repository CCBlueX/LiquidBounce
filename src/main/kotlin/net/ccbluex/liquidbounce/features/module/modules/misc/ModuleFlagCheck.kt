package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.client.gui.screen.DeathScreen
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import org.apache.commons.lang3.StringUtils
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Module Flag Check.
 *
 * Alerts you about set backs.
 */
object ModuleFlagCheck : ClientModule("FlagCheck", Category.MISC, aliases = arrayOf("FlagDetect")) {

    private var chatMessage by boolean("ChatMessage", true)
    private var notification by boolean("Notification", false)
    private var invalidAttributes by boolean("InvalidAttributes", false)

    private object ResetFlags : ToggleableConfigurable(this, "ResetFlags", true) {
        private var afterSeconds by int("After", 30, 1..300, "s")

        @Suppress("unused")
        private val repeatable = tickHandler {
            flagCount = 0
            waitSeconds(afterSeconds)
        }
    }

    private object Render : ToggleableConfigurable(this, "Render", true) {
        private val notInFirstPerson by boolean("NotInFirstPerson", true)
        private val renderTime by int("Alive", 1000, 0..3000, "ms")
        private val fadeOut by easing("FadeOut", Easing.QUAD_OUT)
        private val outTime by int("OutTime", 500, 0..2000, "ms")

        private val alpha by int("Alpha", 100, 0..255)
        private val outlineAlpha by int("OutlineAlpha", 180, 0..255)

        private val colorMode = choices("ColorMode", 2) {
            arrayOf(
                GenericStaticColorMode(it, Color4b.RED),
                GenericRainbowColorMode(it),
                GenericSyncColorMode(it)
            )
        }

        val wireframePlayer = WireframePlayer(Vec3d.ZERO, 0f, 0f)
        var creationTime = 0L
        var finished = true

        override fun onEnabled() {
            finished = true
        }

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> {
            if (finished || (notInFirstPerson && mc.options.perspective.isFirstPerson)) return@handler

            val time = System.currentTimeMillis()
            val withinRenderDuration = time - creationTime < renderTime

            val baseColor = colorMode.activeChoice.getColor(player).withAlpha(alpha)
            val baseOutline = colorMode.activeChoice.getColor(player).withAlpha(outlineAlpha)

            if (withinRenderDuration) {
                wireframePlayer.render(it, baseColor, baseOutline)
            } else {
                val factor = 1f - fadeOut.getFactor(creationTime + renderTime, time, outTime.toFloat())
                if (factor == 0f) {
                    finished = true
                    return@handler
                }
                wireframePlayer.render(it, baseColor.fade(factor), baseOutline.fade(factor))
            }
        }

        fun reset() {
            creationTime = System.currentTimeMillis()
            finished = false
        }
    }

    init {
        tree(ResetFlags)
        tree(Render)
    }

    private var flagCount = 0
    private var lastYaw = 0f
    private var lastPitch = 0f

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (player.age <= 25) return@handler

        when (val packet = event.packet) {
            is PlayerPositionLookS2CPacket -> {
                val change = packet.change
                val deltaYaw = calculateAngleDelta(change.yaw, lastYaw)
                val deltaPitch = calculateAngleDelta(change.pitch, lastPitch)

                flagCount++
                if (deltaYaw >= 90 || deltaPitch >= 90) {
                    alert(AlertReason.FORCEROTATE, "(${deltaYaw.roundToLong()}° | ${deltaPitch.roundToLong()}°)")
                } else {
                    alert(AlertReason.LAGBACK)
                }

                Render.reset()
                val position = change.position
                Render.wireframePlayer.setPosRot(position.x, position.y, position.z, change.yaw, change.pitch)

                lastYaw = player.headYaw
                lastPitch = player.pitch
            }

            is DisconnectS2CPacket -> {
                flagCount = 0
            }
        }
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (!invalidAttributes) return@tickHandler

        val invalidHealth = player.health <= 0f && player.isAlive
        val invalidHunger = player.hungerManager.foodLevel <= 0

        if (!invalidHealth && !invalidHunger) return@tickHandler

        val invalidReasons = mutableListOf<String>()
        if (invalidHealth) invalidReasons.add("Health")
        if (invalidHunger) invalidReasons.add("Hunger")

        if (invalidReasons.isNotEmpty()) {
            flagCount++
            val reasonString = invalidReasons.joinToString()
            alert(AlertReason.INVALID, reasonString)
        }
    }

    private fun alert(reason: AlertReason, extra: String? = null) {

        if (mc.currentScreen is DeathScreen) return

        val message = if (extra == null) {
            message("alert", message(reason.key), flagCount)
        } else {
            message("alertWithExtra", message(reason.key), extra, flagCount)
        }

        if (notification) notification(name, message, NotificationEvent.Severity.INFO)
        if (chatMessage) chat(message, metadata = MessageMetadata(id = "$name#${reason.key}"))
    }

    private fun calculateAngleDelta(newAngle: Float, oldAngle: Float): Float {
        var delta = newAngle - oldAngle
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return abs(delta)
    }

    private enum class AlertReason(val key: String) {
        INVALID("invalid"),
        FORCEROTATE("forceRotate"),
        LAGBACK("lagback")
    }
}
