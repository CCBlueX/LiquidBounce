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
		if (!mc.gameSettings.keyBindJump.isKeyDown)
			return

		// Let's bring back the particles, this mode is useless anyway
        mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FLAME.particleID, player.posX, player.posY + 0.2, player.posZ, -player.motionX, -0.5, -player.motionZ)

		player.motionY += 0.15

		player.motionX *= 1.1
		player.motionZ *= 1.1
	}
}
