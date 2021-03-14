/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.api.minecraft.util.ISession
import net.minecraft.util.Session

class SessionImpl(val wrapped: Session) : ISession
{
	override val profile: GameProfile
		get() = wrapped.profile
	override val username: String
		get() = wrapped.username
	override val playerId: String
		get() = wrapped.playerID
	override val sessionType: String
		get() = wrapped.sessionType.name
	override val token: String
		get() = wrapped.token
	override val sessionID: String
		get() = wrapped.sessionID

	override fun equals(other: Any?): Boolean = other is SessionImpl && other.wrapped == wrapped
}

fun ISession.unwrap(): Session = (this as SessionImpl).wrapped
fun Session.wrap(): ISession = SessionImpl(this)
