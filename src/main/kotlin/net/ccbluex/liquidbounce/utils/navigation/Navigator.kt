/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.agent.v26_3.toAgent
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FINAL_DECISION
import net.ccbluex.liquidbounce.utils.render.route.RouteEvents
import net.ccbluex.liquidbounce.utils.render.route.RouteRenderer
import net.ccbluex.liquidbounce.utils.render.route.RouteView
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.phys.Vec3
import net.ccbluex.agent.navigation.Navigator as AgentNavigator

/**
 * Walks the player to a goal given every tick with Minecraft Agent's navigator, moving it through a
 * [LocalPlayerBody]: straight on where nothing is in the way, else along a searched path, opening doors and, when
 * allowed, breaking and placing blocks. A ridden vehicle is driven there instead, and the player flies or glides
 * where it can.
 */
class Navigator(
    private val owner: EventListener,
    values: ValueGroup,
    rotations: RotationsValueGroup? = null,
    allowBreak: Boolean = false,
    allowPlace: Boolean = false,
    allowParkour: Boolean = true,
    showRoute: Boolean = true,
) : EventListener {

    private val settings = Settings(values, allowBreak, allowPlace, allowParkour)
    private val body = LocalPlayerBody(owner, rotations ?: values.tree(RotationsValueGroup(owner)))
    private val view = RouteView()
    private val navigator = AgentNavigator(body, settings.options(), RouteEvents(view))

    init {
        values.tree(RouteRenderer(owner, view, showRoute))
    }

    /** Whether this tick's keys must reach the player as they are. */
    private var exactKeys = false

    val isActing: Boolean
        get() = navigator.isActing

    /** Whether the walk holds the player at ledges now, by sneaking there. */
    val isSafeWalking: Boolean
        get() = body.safeWalk

    /** Where the walk heads: the aim of a block action, else the way to the next point, else the movement input. */
    fun movementRotation(): Rotation = navigator.movementRotation().let { Rotation(it.yRot, it.xRot) }

    /**
     * Moves one tick towards [goal], within [radius]; true if the movement is a plain walk other features may add
     * to. Call from a [MovementInputEvent] handler at [CRITICAL_MODIFICATION].
     */
    fun navigate(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        navigator.options = settings.options()
        val plainWalk = navigator.navigate(goal.toAgent(), radius)
        body.applyKeys(event)
        exactKeys = body.keysExact
        return plainWalk
    }

    /** For ticks without a goal; the keys are the user's again. */
    fun halt() {
        navigator.halt()
        body.release()
        exactKeys = false
    }

    fun reset() {
        navigator.stop()
        body.release()
        exactKeys = false
    }

    // Exact keys, such as a parkour jump or a climb, reach the player as the navigator pressed them
    @Suppress("unused")
    private val exactInputHandler = handler<MovementInputEvent>(priority = FINAL_DECISION) { event ->
        if (exactKeys) {
            body.applyKeys(event)
        }
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (body.wantsSprint && event.directionalInput.isMoving &&
            (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT)
        ) {
            event.sprint = true
        }
    }

    @Suppress("unused")
    private val blockChangeHandler = handler<BlockChangeEvent> { event ->
        mc.execute {
            val pos = event.blockPos.toAgent()
            navigator.blockChanged(pos)
            navigator.blockConfirmed(pos, event.newState.toAgent())
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin == TransferOrigin.INCOMING && event.packet is ClientboundPlayerPositionPacket) {
            mc.execute(navigator::positionCorrected)
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        navigator.levelChanged()
        body.release()
    }

    override fun parent() = owner

}
