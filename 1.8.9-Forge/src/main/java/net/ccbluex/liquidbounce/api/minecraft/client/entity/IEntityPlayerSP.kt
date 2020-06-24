/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.entity

@Suppress("INAPPLICABLE_JVM_NAME")
interface IEntityPlayerSP : IAbstractClientPlayer {
    @get:JvmName("isSneaking")
    val sneaking: Boolean
    var serverSprintState: Boolean

    fun sendChatMessage(msg: String)
    fun respawnPlayer()
}