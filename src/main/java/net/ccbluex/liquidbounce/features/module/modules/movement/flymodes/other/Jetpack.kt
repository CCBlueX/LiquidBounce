/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.minecraft.util.EnumParticleTypes

object Jetpack : FlyMode("Jetpack") {
	override fun onUpdate() {
		if (!mc.options.jumpKey.isPressed)
			return

		// Let's bring back the particles, this mode is useless anyway
        mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FLAME.particleID, mc.player.x, mc.player.z + 0.2, mc.player.z, -mc.player.velocityX, -0.5, -mc.player.velocityZ)

		mc.player.velocityY += 0.15

		mc.player.velocityX *= 1.1
		mc.player.velocityZ *= 1.1
	}
}
