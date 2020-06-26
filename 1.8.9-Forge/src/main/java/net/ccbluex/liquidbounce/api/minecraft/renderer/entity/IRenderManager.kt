/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.renderer.entity

interface IRenderManager {
    val viewerPosX: Double
    val viewerPosY: Double
    val viewerPosZ: Double
    val playerViewX: Float
    val playerViewY: Float
    val renderPosX: Double
    val renderPosY: Double
    val renderPosZ: Double
}