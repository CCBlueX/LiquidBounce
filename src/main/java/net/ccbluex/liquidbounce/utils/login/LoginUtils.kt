/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.login

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.util.Session
import java.util.*

fun me.liuli.elixir.compat.Session.intoMinecraftSession(): Session = Session(username, uuid, token, type)

object LoginUtils : MinecraftInstance()
{
    @JvmStatic
    fun loginSessionId(sessionId: String): LoginResult
    {
        val decodedSessionData = try
        {
            String(Base64.getDecoder().decode(sessionId.split(".")[1]), Charsets.UTF_8)
        }
        catch (e: Exception)
        {
            return LoginResult.FAILED_PARSE_SESSION
        }

        val sessionObject = try
        {
            JsonParser().parse(decodedSessionData).asJsonObject
        }
        catch (e: java.lang.Exception)
        {
            return LoginResult.FAILED_PARSE_SESSION
        }

        val uuid = sessionObject.get("spr")?.asString ?: return LoginResult.FAILED_PARSE_SESSION
        val accessToken = sessionObject.get("yggt")?.asString ?: return LoginResult.FAILED_PARSE_SESSION

        if (!UserUtils.isValidToken(accessToken)) return LoginResult.INVALID_ACCOUNT_DATA
        val username = UserUtils.getUsername(uuid) ?: return LoginResult.INVALID_ACCOUNT_DATA

        mc.session = Session(username, uuid, accessToken, "mojang")
        LiquidBounce.eventManager.callEvent(SessionEvent())

        return LoginResult.LOGGED_IN
    }

    enum class LoginResult
    {
        INVALID_ACCOUNT_DATA,
        FAILED_PARSE_SESSION,
        LOGGED_IN
    }
}
