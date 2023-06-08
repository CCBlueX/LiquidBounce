/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.vitox.ParticleGenerator

@SideOnly(Side.CLIENT)
object ParticleUtils {
    private val particleGenerator = ParticleGenerator(100)

    fun drawParticles(mouseX: Int, mouseY: Int) = particleGenerator.draw(mouseX, mouseY)
}