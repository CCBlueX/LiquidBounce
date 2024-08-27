/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.jumpY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.block.LadderBlock
import net.minecraft.block.material.Material
import net.minecraft.util.Box

object Jump : FlyMode("Jump") {

    override fun onUpdate() {
        if (mc.player == null)
            return
        if (mc.player.onGround && !mc.player.jumping)
            mc.player.tryJump()
        if ((mc.options.jumpKey.isPressed && !mc.options.sneakKey.isPressed) || mc.player.onGround)
            jumpY = mc.player.z
    }

    override fun onBB(event: BlockBBEvent) {
        val jumpYCondition = if (!mc.options.jumpKey.isPressed && mc.options.sneakKey.isPressed) event.y.toDouble() < jumpY else event.y.toDouble() <= jumpY
        if ((!event.block.material.blocksMovement() && event.block.material != Material.carpet && event.block.material != Material.vine && event.block.material != Material.snow && event.block !is LadderBlock) && jumpYCondition) {
            event.boundingBox = Box.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x.toDouble() + 1,
                1.0,
                event.z.toDouble() + 1
            )
        }
    }
}
