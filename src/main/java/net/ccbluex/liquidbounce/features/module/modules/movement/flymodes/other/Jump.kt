/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.jumpY
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.injection.forge.mixins.entity.MixinEntity
import net.ccbluex.liquidbounce.injection.forge.mixins.entity.MixinEntityLivingBase
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.block.BlockLadder
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks.air
import net.minecraft.util.AxisAlignedBB
import kotlin.math.floor

object Jump : FlyMode("Jump") {

    override fun onUpdate() {
        if (mc.thePlayer == null)
            return
        if (mc.thePlayer.onGround && !mc.thePlayer.isJumping)
            mc.thePlayer.jump()
        if ((mc.gameSettings.keyBindJump.isKeyDown && !mc.gameSettings.keyBindSneak.isKeyDown) || mc.thePlayer.onGround)
            jumpY = mc.thePlayer.posY
    }

    override fun onBB(event: BlockBBEvent) {
        val jumpYCondition = if (!mc.gameSettings.keyBindJump.isKeyDown && mc.gameSettings.keyBindSneak.isKeyDown) event.y.toDouble() < jumpY else event.y.toDouble() <= jumpY
        if ((!event.block.material.blocksMovement() && event.block.material != Material.carpet && event.block.material != Material.vine && event.block.material != Material.snow && event.block !is BlockLadder) && jumpYCondition) {
            event.boundingBox = AxisAlignedBB.fromBounds(
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
