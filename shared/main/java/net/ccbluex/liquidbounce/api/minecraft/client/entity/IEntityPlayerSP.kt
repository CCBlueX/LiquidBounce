/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.entity

import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.ccbluex.liquidbounce.api.minecraft.util.IMovementInput

@Suppress("INAPPLICABLE_JVM_NAME")
interface IEntityPlayerSP : IAbstractClientPlayer {
    var horseJumpPowerCounter: Int
    var horseJumpPower: Float

    val sendQueue: IINetHandlerPlayClient
    val movementInput: IMovementInput

    var serverSprintState: Boolean

    fun sendChatMessage(msg: String)
    fun respawnPlayer()
    fun addChatMessage(component: IIChatComponent)
    fun closeScreen()
}