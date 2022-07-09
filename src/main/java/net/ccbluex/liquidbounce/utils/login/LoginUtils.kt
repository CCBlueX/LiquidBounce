/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.login

import com.google.gson.JsonParser
import com.mojang.authlib.Agent
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication
import com.thealtening.AltService
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.login.UserUtils.getUUID
import net.mcleaks.MCLeaks
import net.mcleaks.RedeemResponse
import java.net.Proxy
import java.util.*

object LoginUtils : MinecraftInstance()
{

    @JvmStatic
    fun login(serviceType: MinecraftAccount.AltServiceType, username: String, password: String?): LoginResult
    {

        // MCLeaks
        if (MinecraftAccount.AltServiceType.MCLEAKS.equals(serviceType))
        {
            if (username.length != 16) return LoginResult.INVALID_ACCOUNT_DATA

            var result: LoginResult = LoginResult.AUTHENTICATION_FAILURE
            MCLeaks.redeem(username) {
                if (it is String) result = if (it.startsWith("An error occurred!", true)) LoginResult.AUTHENTICATION_FAILURE else LoginResult.MCLEAKS_INVALID
                else if (it is RedeemResponse)
                {
                    MCLeaks.refresh(net.mcleaks.Session(it.username, it.token))

                    try
                    {
                        GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)
                    }
                    catch (e: Exception)
                    {
                        ClientUtils.logger.error("Failed to switch back alt service to Mojang.", e)
                    }
                    result = LoginResult.LOGGED_IN
                }
            }
            return result
        }

        val savedAltService = GuiAltManager.altService.currentService

        when (serviceType)
        {
            MinecraftAccount.AltServiceType.THEALTENING, MinecraftAccount.AltServiceType.THEALTENING_INVALID -> GuiAltManager.altService.switchService(AltService.EnumAltService.THEALTENING)
            else -> GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)
        }

        val userAuthentication = YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT) as YggdrasilUserAuthentication

        userAuthentication.setUsername(username)
        if (MinecraftAccount.AltServiceType.MOJANG.equals(serviceType)) userAuthentication.setPassword(password)
        else userAuthentication.setPassword(LiquidBounce.CLIENT_NAME) // xd

        return try
        {
            userAuthentication.logIn()
            mc.session = Session(userAuthentication.selectedProfile.name, userAuthentication.selectedProfile.id.toString(), userAuthentication.authenticatedToken, "mojang")
            LiquidBounce.eventManager.callEvent(SessionEvent())
            MCLeaks.remove()
            LoginResult.LOGGED_IN
        }
        catch (exception: AuthenticationUnavailableException)
        {
            try
            {
                GuiAltManager.altService.switchService(savedAltService)
            }
            catch (e: Exception)
            {
                ClientUtils.logger.error("Failed to switch back alt service.", e)
            }

            LoginResult.AUTHENTICATION_UNAVAILABLE
        }
        catch (exception: AuthenticationException)
        {
            try
            {
                GuiAltManager.altService.switchService(savedAltService)
            }
            catch (e: Exception)
            {
                ClientUtils.logger.error("Failed to switch back alt service.", e)
            }

            val message = exception.message ?: return LoginResult.AUTHENTICATION_FAILURE

            when
            {
                (MinecraftAccount.AltServiceType.THEALTENING.equals(serviceType)) -> LoginResult.THEALTENING_INVALID
                message.contains("invalid username or password.", ignoreCase = true) -> LoginResult.INVALID_ACCOUNT_DATA
                message.contains("account migrated", ignoreCase = true) -> LoginResult.MIGRATED
                else -> LoginResult.AUTHENTICATION_FAILURE
            }
        }
        catch (exception: NullPointerException)
        {
            LoginResult.AUTHENTICATION_FAILURE
        }
    }

    @JvmStatic
    fun loginCracked(username: String?)
    {
        mc.session = Session(username ?: return, getUUID(username), "-", "legacy")
        LiquidBounce.eventManager.callEvent(SessionEvent())
    }

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

        val uuid = sessionObject.get("spr").asString
        val accessToken = sessionObject.get("yggt").asString

        if (accessToken !is ValidToken) return LoginResult.INVALID_ACCOUNT_DATA
        val username = UserUtils.getUsername(uuid) ?: return LoginResult.INVALID_ACCOUNT_DATA

        mc.session = Session(username, uuid, accessToken, "mojang")
        LiquidBounce.eventManager.callEvent(SessionEvent())

        return LoginResult.LOGGED_IN
    }

    enum class LoginResult
    {
        INVALID_ACCOUNT_DATA,
        AUTHENTICATION_FAILURE,
        AUTHENTICATION_UNAVAILABLE,
        MIGRATED,
        MCLEAKS_INVALID,
        THEALTENING_INVALID,
        FAILED_PARSE_SESSION,
        LOGGED_IN
    }
}
