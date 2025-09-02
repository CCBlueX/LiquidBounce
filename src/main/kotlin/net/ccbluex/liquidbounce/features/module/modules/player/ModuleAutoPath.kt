package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.pathing.PathManagers
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.pathing.BaritonePathManager

object ModuleAutoPath : ClientModule("AutoPath", Category.PLAYER) {
    private var targetPos: BlockPos? = null

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!mc.options.pickItemKey.isPressed) return@tickHandler

        val ray = raycast(player.rotation, 100.0)
        val newTarget = ray.blockPos.up()

        if (targetPos != null && targetPos != newTarget) {
            PathManagers.get().stop()
        }

        targetPos = newTarget
    }

    @Suppress("unused")
    private val moveHandler = handler<MovementInputEvent>(CRITICAL_MODIFICATION) { event ->
        val target = targetPos ?: return@handler

        if (player.pos.distanceTo(Vec3d.ofCenter(target)) < 0.1) {
            PathManagers.get().stop()
            targetPos = null
            return@handler
        }

        if (PathManagers.get().currentTarget != target) {
            PathManagers.get().moveTo(target, false)
        }
    }


    init{
        tree(BaritonePathManager(this))
    }

    override fun onEnabled() {
        PathManagers.init()
        targetPos = null
        super.onEnabled()
    }
    override fun onDisabled() {
        PathManagers.get().stop()
        targetPos = null
        super.onDisabled()
    }
}
