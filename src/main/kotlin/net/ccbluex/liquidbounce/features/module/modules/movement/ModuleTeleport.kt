package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.ModuleDisabler
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.floor

object ModuleTeleport : Module("Teleport", Category.MOVEMENT, aliases = arrayOf("tp"), disableActivation = true) {

    private val allFull by boolean("AllFullPacket", false)

    private val paperExploit by boolean("PaperBypass", false)

    val highTp by boolean("HighTP", false)
    val highTpAmount by float("HighTPAmount", 200.0F, 0.0F..500.0F)

    private val groundMode by enumChoice("GroundMode", GroundMode.CORRECT)

    private val resetMotion by boolean("ResetMotion", true)

    private val waitForTeleport by boolean("WaitForTeleport", false)

    private val enableDisabler by boolean("EnableDisablerOnWait", false)

    enum class GroundMode(override val choiceName: String) : NamedChoice {
        TRUE("True"),
        FALSE("False"),
        CORRECT("Correct")
    }

    private var indicatedTeleport: Vec3d? = null
    private var teleportsToWait: Int = 0

    @Suppress("CognitiveComplexMethod") // it really isn't, but detekt is determined to complain about it
    fun indicateTeleport(x: Double = player.x, y: Double = player.y, z: Double = player.z, teleportsToWait: Int = 0) {
        if (waitForTeleport) {
            indicatedTeleport = Vec3d(x, y, z)
            this.teleportsToWait = teleportsToWait

            if (teleportsToWait == 1 && enableDisabler) {
                ModuleDisabler.enabled = true
            }
        } else {
            teleport(x, y, z)
        }
    }

    @Suppress("unused")
    val packetHandler = handler<PacketEvent> {
        if (indicatedTeleport == null) return@handler

        if (mc.world != null && it.origin == TransferOrigin.RECEIVE) {
            if (it.packet is PlayerPositionLookS2CPacket) {
                if (teleportsToWait > 1) {
                    teleportsToWait--
                    chat("$teleportsToWait teleports to go :3")
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

                if (enableDisabler) {
                    ModuleDisabler.enabled = false
                }
            }
        }
    }

    @Suppress("CognitiveComplexMethod") // it really isn't, but detekt is determined to complain about it
    fun teleport(x: Double = player.x, y: Double = player.y, z: Double = player.z) {

        chat("teleport :3")

        val deltaX = x - player.x
        val deltaY = y - player.y
        val deltaZ = z - player.z

        if (paperExploit) {
            repeat((floor((abs(deltaX) + abs(deltaY) + abs(deltaZ)) / 10) - 1).toInt()) {
                network.sendPacket((
                        if (allFull) {
                            MovePacketType.FULL
                        } else {
                            MovePacketType.POSITION_AND_ON_GROUND
                        }).generatePacket().apply {
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
                    }
                )
            }

            network.sendPacket((
                if (allFull) {
                    MovePacketType.FULL
                } else {
                    MovePacketType.POSITION_AND_ON_GROUND
                }).generatePacket().apply {
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

    }

    override fun handleEvents(): Boolean = !isDestructed

}
