/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.entity

interface IEntityFishHook : IEntity
{
    val inGround: Boolean
    val caughtEntity: IEntity?
}
