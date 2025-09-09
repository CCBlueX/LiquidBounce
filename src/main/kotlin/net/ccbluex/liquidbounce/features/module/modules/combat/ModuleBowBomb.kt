package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.MathHelper
import java.util.*

object ModuleBowBomb : ClientModule("BowBomb", Category.COMBAT, aliases = arrayOf("OneShot")){
    private val spoofs by int("Spoofs", 50, 0..200)
    private val delay by float("Delay", 5f, 0f..10f, "s")
    private val activeTime by float("ActiveTime", 0.4f, 0f..3f, "s")
    private val minimize by boolean("Minimize", false)
    private val message by boolean("Message", true)
    private val exploit by enumChoice("Exploit", ExploitMode.STRICT, ExploitMode.entries.toSet())

    private val targetTracker = TargetTracker(TargetPriority.DISTANCE)
    private val rotationsConfigurable = tree(RotationsConfigurable(this))
    private val delayTimer = Chronometer()
    private val activeTimer = Chronometer()
    private val random = Random()
    private var active = false

    private enum class ExploitMode(override val choiceName: String) : NamedChoice {
        STRONG("Strong"),
        FAST("Fast"),
        STRICT("Strict"),
        PHOBOS("Phobos")
    }

    init {
        tree(targetTracker)
        tree(rotationsConfigurable)
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (player.isUsingItem && player.activeItem.item == Items.BOW && player.itemUseTime > 0) {
            if (activeTimer.elapsed == 0L) activeTimer.reset()
        } else {
            activeTimer.reset()
            return@tickHandler
        }

        targetTracker.reset()
        if (player.activeItem.item != Items.BOW) return@tickHandler

        val projectileInfo = TrajectoryData.getRenderedTrajectoryInfo(
            player,
            player.activeItem.item,
            true
        ) ?: return@tickHandler

        var rotation: Rotation? = null
        targetTracker.selectFirst { enemy ->
            rotation = SituationalProjectileAngleCalculator.calculateAngleForEntity(projectileInfo, enemy)
            rotation != null
        } ?: return@tickHandler

        RotationManager.setRotationTarget(
            rotation!!,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = ModuleBowBomb,
            configurable = rotationsConfigurable
        )
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!player.isUsingItem || player.activeItem.item != Items.BOW) {
            activeTimer.reset()
            active = false
        } else {
            active = true
        }
    }
    @Suppress("unused","ComplexCondition")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is PlayerActionC2SPacket && packet.action == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM &&
            player.activeItem.item == Items.BOW &&
            delayTimer.hasElapsed((delay * 1000).toLong()) &&
            activeTimer.hasElapsed((activeTime * 1000).toLong()) &&
            active
        ) {
            if (message) chat("§rBomb")
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING))
            val runs = spoofs
            repeat(runs) {
                when (exploit) {
                    ExploitMode.FAST -> {
                        spoof(player.x, if (minimize) player.y else player.y - 1e-10, player.z, true)
                        spoof(player.x, player.y + 1e-10, player.z, false)
                    }
                    ExploitMode.STRONG -> {
                        spoof(player.x, player.y + 1e-10, player.z, false)
                        spoof(player.x, if (minimize) player.y else player.y - 1e-10, player.z, true)
                    }
                    ExploitMode.PHOBOS -> {
                        spoof(player.x, player.y + 0.00000000000013, player.z, true)
                        spoof(player.x, player.y + 0.00000000000027, player.z, false)
                    }
                    ExploitMode.STRICT -> {
                        val angle = Math.toRadians(player.yaw.toDouble())
                        val dx = -MathHelper.sin(angle.toFloat()) * 100.0
                        val dz = MathHelper.cos(angle.toFloat()) * 100.0
                        if (random.nextBoolean()) {
                            spoof(player.x - dx, player.y, player.z - dz, false)
                        } else {
                            spoof(player.x + dx, player.y, player.z + dz, true)
                        }
                    }
                }
            }
            delayTimer.reset()
        }
    }

    private fun spoof(x: Double, y: Double, z: Double, ground: Boolean) {
        val offset = if (minimize) 0.0 else random.nextDouble() * 1e-10
        val packet = PlayerMoveC2SPacket.PositionAndOnGround(
            x, y + offset, z, ground, player.horizontalCollision)

        network.sendPacket(packet)
    }
}
