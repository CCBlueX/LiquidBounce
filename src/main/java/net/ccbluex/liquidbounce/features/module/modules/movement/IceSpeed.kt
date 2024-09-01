/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getMaterial
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos

object IceSpeed : Module("IceSpeed", Category.MOVEMENT) {
    private val mode by ListValue("Mode", arrayOf("NCP", "AAC", "Spartan"), "NCP")
    override fun onEnable() {
        if (mode == "NCP") {
            Blocks.ICE.slipperiness = 0.39f
            Blocks.PACKED_ICE.slipperiness = 0.39f
        }
        super.onEnable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val mode = mode
        if (mode == "NCP") {
            Blocks.ICE.slipperiness = 0.39f
            Blocks.PACKED_ICE.slipperiness = 0.39f
        } else {
            Blocks.ICE.slipperiness = 0.98f
            Blocks.PACKED_ICE.slipperiness = 0.98f
        }

        val player = mc.player ?: return

        if (player.onGround && !player.isClimbing && !player.isSneaking && player.isSprinting && isMoving) {
            if (mode == "AAC") {
                getMaterial(player.pos.down()).let {
                    if (it == Blocks.ICE || it == Blocks.PACKED_ICE) {
                        player.velocityX *= 1.342
                        player.velocityZ *= 1.342
                        Blocks.ICE.slipperiness = 0.6f
                        Blocks.PACKED_ICE.slipperiness = 0.6f
                    }
                }
            }
            if (mode == "Spartan") {
                getMaterial(player.pos.down()).let {
                    if (it == Blocks.ICE || it == Blocks.PACKED_ICE) {
                        val upBlock = getBlock(BlockPos(player).up(2))

                        if (upBlock != Blocks.AIR) {
                            player.velocityX *= 1.342
                            player.velocityZ *= 1.342
                        } else {
                            player.velocityX *= 1.18
                            player.velocityZ *= 1.18
                        }

                        Blocks.ICE.slipperiness = 0.6f
                        Blocks.PACKED_ICE.slipperiness = 0.6f
                    }
                }
            }
        }
    }

    override fun onDisable() {
        Blocks.ICE.slipperiness = 0.98f
        Blocks.PACKED_ICE.slipperiness = 0.98f
        super.onDisable()
    }
}