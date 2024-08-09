package net.ccbluex.liquidbounce.features.module.modules.bmw

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ModuleTellyBridge : Module("TellyBridge", Category.BMW) {

    private val timeToScaffoldAfterJump by int("TimeToScaffoldAfterJump",
        2, 0..10, "ticks")
    private val autoJump by boolean("AutoJump", true)

    private var onGroundTick = 0
    private var inAirTick = 0

    val keyEventHandler = handler<KeyEvent> {
        ModuleScaffold.sameYMode = if (mc.options.jumpKey.isPressed) {
            ModuleScaffold.SameYMode.OFF
        } else {
            ModuleScaffold.SameYMode.ON
        }
    }

    val gameTickEventHandler = handler<GameTickEvent> {
        onGroundTick++
        inAirTick++
    }

    val movementInputEventHandler = handler<MovementInputEvent> {
        if (autoJump) {
            it.jumping = true
        }
        if (player.isOnGround) {
            inAirTick = 0
            ModuleScaffold.enabled = if (autoJump) { onGroundTick >= 3 } else { !mc.options.jumpKey.isPressed }
        } else {
            onGroundTick = 0
            ModuleScaffold.enabled = inAirTick >= timeToScaffoldAfterJump
        }
    }

    override fun enable() {
        onGroundTick = 3
        inAirTick = 0
    }

    override fun disable() {
        ModuleScaffold.enabled = false
    }
}
