package net.ccbluex.liquidbounce.features.module.modules.movement

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.ModuleDisabler
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.floor

object ModuleTeleport : Module("Teleport", Category.EXPLOIT, aliases = arrayOf("tp")) {

    private val allFull by boolean("AllFullPacket", false)
    private val paperExploit by boolean("PaperBypass", false)
    val highTp by boolean("HighTP", false)
    val highTpAmount by float("HighTPAmount", 200.0F, 0.0F..500.0F)
    private val groundMode by enumChoice("GroundMode", GroundMode.CORRECT)
    private val resetMotion by boolean("ResetMotion", true)

    private val functionAfterServerTeleport by int("FunctionAfterTeleports", 0, 0..5)
    private val withDisabler by boolean("WithDisablerOnWait", false)

    private val decimalFormat = DecimalFormat("##0.000")

    enum class GroundMode(override val choiceName: String) : NamedChoice {
        TRUE("True"),
        FALSE("False"),
        CORRECT("Correct")
    }

    private var indicatedTeleport: Vec3d? = null
    private var teleportsToWait: Int = 0

    override fun enable() {
        if (indicatedTeleport == null) {
            chat(warning(message("useCommand")))

            // Disables module on next render tick
            RenderSystem.recordRenderCall {
                this.enabled = false
            }
        }

        super.enable()
    }

    override fun disable() {
        indicatedTeleport = null
        teleportsToWait = 0
        super.disable()
    }

    fun indicateTeleport(x: Double = player.x, y: Double = player.y, z: Double = player.z) {
        if (functionAfterServerTeleport == 0) {
            teleport(x, y, z)
            return
        }

        this.indicatedTeleport = Vec3d(x, y, z)
        this.teleportsToWait = functionAfterServerTeleport
        this.enabled = true

        if (teleportsToWait == 1 && withDisabler) {
            ModuleDisabler.enabled = true
        }

        chat(variable(message("teleportsLeft", teleportsToWait)))
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        if (indicatedTeleport == null) {
            return@handler
        }

        if (mc.world != null && it.origin == TransferOrigin.RECEIVE) {
            if (it.packet is PlayerPositionLookS2CPacket) {
                if (teleportsToWait > 1) {
                    teleportsToWait--
                    chat(variable(message("teleportsLeft", teleportsToWait)))
                    return@handler
                }

                sendPacketSilently(MovePacketType.FULL.generatePacket().apply {
                    this.x = it.packet.x
                    this.y = it.packet.y
                    this.z = it.packet.z
                    this.yaw = it.packet.yaw
                    this.pitch = it.packet.pitch
                    this.onGround = false
                })

                teleport(indicatedTeleport!!.x, indicatedTeleport!!.y, indicatedTeleport!!.z)
                indicatedTeleport = null
                it.cancelEvent()

                if (withDisabler) {
                    ModuleDisabler.enabled = false
                }
            }
        }
    }

    fun teleport(x: Double = player.x, y: Double = player.y, z: Double = player.z) {
        val deltaX = x - player.x
        val deltaY = y - player.y
        val deltaZ = z - player.z

        if (paperExploit) {
            val times = (floor((abs(deltaX) + abs(deltaY) + abs(deltaZ)) / 10) - 1).toInt()
            val packetToSend = if (allFull) MovePacketType.FULL else MovePacketType.POSITION_AND_ON_GROUND
            repeat(times) {
                network.sendPacket(packetToSend.generatePacket().apply {
                    this.x = player.x
                    this.y = player.y
                    this.z = player.z
                    this.yaw = player.yaw
                    this.pitch = player.pitch
                    this.onGround = when (groundMode) {
                        GroundMode.TRUE -> true
                        GroundMode.FALSE -> false
                        GroundMode.CORRECT -> player.isOnGround
                    }
                })
            }

            network.sendPacket(packetToSend.generatePacket().apply {
                this.x = x
                this.y = y
                this.z = z
                this.yaw = player.yaw
                this.pitch = player.pitch
                this.onGround = when (groundMode) {
                    GroundMode.TRUE -> true
                    GroundMode.FALSE -> false
                    GroundMode.CORRECT -> player.isOnGround
                }
            })
        }

        player.updatePosition(x, y, z)

        if (resetMotion) {
            player.velocity = player.velocity.multiply(0.0, 0.0, 0.0)
        }

        chat(regular(
            message("teleported",
            variable(decimalFormat.format(x)),
            variable(decimalFormat.format(y)),
            variable(decimalFormat.format(z)))
        ))
        this.enabled = false
    }

}
