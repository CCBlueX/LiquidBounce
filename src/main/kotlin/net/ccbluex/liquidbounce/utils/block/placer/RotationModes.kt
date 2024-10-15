package net.ccbluex.liquidbounce.utils.block.placer

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SingleUseAction
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos

abstract class RotationMode(
    name: String,
    private val configurable: ChoiceConfigurable<RotationMode>,
    val placer: BlockPlacer
) : Choice(name), QuickImports {

    abstract operator fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean

    open fun getVerificationRotation(targetedRotation: Rotation) = targetedRotation

    open fun onTickStart() {}

    override val parent: ChoiceConfigurable<*>
        get() = configurable

}

/**
 * Normal rotations.
 * Only one placement per tick is possible, possible less because rotating takes some time.
 */
class NormalRotationMode(configurable: ChoiceConfigurable<RotationMode>, placer: BlockPlacer)
    : RotationMode("Normal", configurable, placer) {

    val rotations = tree(RotationsConfigurable(this))

    override fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean {
        RotationManager.aimAt(
            placementTarget.rotation,
            considerInventory = !placer.ignoreOpenInventory,
            configurable = rotations,
            provider = placer.module,
            priority = placer.priority,
            whenReached = SingleUseAction {
                placer.postRotateTasks.add {
                    placer.doPlacement(isSupport, pos, placementTarget)
                }
            }
        )

        return true
    }

    override fun getVerificationRotation(targetedRotation: Rotation): Rotation = RotationManager.serverRotation

}

/**
 * No rotations, or just a packet containing the rotation target.
 */
class NoRotationMode(configurable: ChoiceConfigurable<RotationMode>, placer: BlockPlacer)
    : RotationMode("None", configurable, placer) {

    val send by boolean("SendRotationPacket", true)

    /**
     * Not rotating properly allows doing multiple placements. "b/o" stands for blocker per operation.
     */
    private val placements by int("Placements", 1, 1..10, "b/o")

    private var placementsDone = 0

    override fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean {
        placer.postRotateTasks.add {
            if (send) {
                val rotation = placementTarget.rotation.fixedSensitivity()
                network.connection!!.send(
                    PlayerMoveC2SPacket.LookAndOnGround(rotation.yaw, rotation.pitch, player.isOnGround),
                    null
                )
            }

            placer.doPlacement(isSupport, pos, placementTarget)
        }

        placementsDone++
        return placementsDone == placements
    }

    override fun onTickStart() {
        placementsDone = 0
    }

}
