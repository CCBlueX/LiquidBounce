/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.login

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.util.Session
import java.util.*

fun me.liuli.elixir.compat.Session.intoMinecraftSession() = Session(username, uuid, token, type)

object LoginUtils : MinecraftInstance() {

    fun loginSessionId(sessionId: String): LoginResult {
        val decodedSessionData = try {
            String(Base64.getDecoder().decode(sessionId.split(".")[1]), Charsets.UTF_8)
        } catch (e: Exception) {
            return LoginResult.FAILED_PARSE_TOKEN
        }

        val sessionObject = try {
            JsonParser().parse(decodedSessionData).asJsonObject
        } catch (e: java.lang.Exception){
            return LoginResult.FAILED_PARSE_TOKEN
        }
        val uuid = sessionObject["spr"]?.asString
        val accessToken = sessionObject["yggt"]?.asString

        if (!accessToken?.let { UserUtils.isValidToken(it) }!!) {
            return LoginResult.INVALID_ACCOUNT_DATA
        }

        val username = uuid?.let { UserUtils.getUsername(it) } ?: return LoginResult.INVALID_ACCOUNT_DATA

        mc.session = Session(username, uuid, accessToken, "mojang")
        callEvent(SessionEvent())

        return LoginResult.LOGGED
    }

    enum class LoginResult {
        INVALID_ACCOUNT_DATA, LOGGED, FAILED_PARSE_TOKEN
    }

}