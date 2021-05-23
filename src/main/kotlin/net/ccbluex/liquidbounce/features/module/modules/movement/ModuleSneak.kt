package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

object ModuleSneak : Module("Sneak", Category.MOVEMENT) {

    var modes = choices("Mode", Vanilla, arrayOf(Legit, Vanilla, Switch))
    var stopMove by boolean("StopMove", false)
    var sneaking = false

    private object Legit : Choice("Legit") {

        override val parent: ChoiceConfigurable
            get() = modes

        val networkTick = handler<PlayerNetworkMovementTickEvent> {
            if(stopMove && player.moving) {
                if(sneaking) {
                    disable()
                } else return@handler
            }
            mc.options.keySneak.isPressed = true
        }

        override fun disable() {
            mc.options.keySneak.isPressed = false
            sneaking = false
        }
    }

    private object Vanilla : Choice("Vanilla") {

        override val parent: ChoiceConfigurable
            get() = modes

        val networkTick = handler<PlayerNetworkMovementTickEvent> {
            if(stopMove && player.moving) {
                if(sneaking) {
                    disable()
                } else return@handler
            }
            if (!sneaking) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
            }
        }

        override fun disable() {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
            sneaking = false
        }
    }

    private object Switch : Choice("Switch") {

        override val parent: ChoiceConfigurable
            get() = modes

        val networkTick = handler<PlayerNetworkMovementTickEvent> { event ->
            if(stopMove && player.moving) {
                if(sneaking) {
                    disable()
                } else return@handler
            }
            when(event.state) {
                EventState.PRE -> {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                }
                EventState.POST -> {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                }
            }
        }

        override fun disable() {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
            sneaking = false
        }
    }
}
