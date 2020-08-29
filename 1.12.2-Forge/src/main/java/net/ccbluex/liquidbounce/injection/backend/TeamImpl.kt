/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.scoreboard.ITeam
import net.ccbluex.liquidbounce.api.minecraft.util.WEnumChatFormatting
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.scoreboard.Team

class TeamImpl(val wrapped: Team) : ITeam {
    override val chatFormat: WEnumChatFormatting
        get() = (wrapped as ScorePlayerTeam).color.wrap()

    override fun formatString(name: String): String = wrapped.formatString(name)

    override fun isSameTeam(team: ITeam): Boolean = wrapped.isSameTeam(team.unwrap())

    override fun equals(other: Any?): Boolean {
        return other is TeamImpl && other.wrapped == this.wrapped
    }
}

inline fun ITeam.unwrap(): Team = (this as TeamImpl).wrapped
inline fun Team.wrap(): ITeam = TeamImpl(this)