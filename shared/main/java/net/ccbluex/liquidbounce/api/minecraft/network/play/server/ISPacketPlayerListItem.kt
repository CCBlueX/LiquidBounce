/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.server

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings

interface ISPacketPlayerListItem : IPacket
{
    val action: WAction
    val players: MutableList<WAddPlayerData>

    enum class WAction
    {
        ADD_PLAYER,
        UPDATE_GAME_MODE,
        UPDATE_LATENCY,
        UPDATE_DISPLAY_NAME,
        REMOVE_PLAYER;
    }

    data class WAddPlayerData(val profile: GameProfile, val ping: Int, val gameMode: IWorldSettings.WGameType?, val displayName: IIChatComponent?)
}
