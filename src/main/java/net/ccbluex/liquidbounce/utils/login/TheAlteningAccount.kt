package net.ccbluex.liquidbounce.utils.login

import com.google.gson.JsonObject
import com.mojang.authlib.Agent
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.compat.Session
import me.liuli.elixir.exception.LoginException
import me.liuli.elixir.utils.set
import me.liuli.elixir.utils.string
import net.ccbluex.liquidbounce.LiquidBounce
import java.net.Proxy

class TheAlteningAccount : MinecraftAccount("TheAltening")
{
    override var name = ""
    var token = ""
    private var uuid = ""
    private var accessToken = ""

    override val session: Session
        get()
        {
            if (name.isEmpty() || uuid.isEmpty() || accessToken.isEmpty())
            {
                update()
            }

            return Session(name, uuid, accessToken, "mojang")
        }

    override fun update()
    {
        val userAuthentication = YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT) as YggdrasilUserAuthentication

        userAuthentication.setUsername(token)
        userAuthentication.setPassword(LiquidBounce.CLIENT_NAME)

        try
        {
            userAuthentication.logIn()
            name = userAuthentication.selectedProfile.name
            uuid = userAuthentication.selectedProfile.id.toString()
            accessToken = userAuthentication.authenticatedToken
        }
        catch (exception: AuthenticationUnavailableException)
        {
            throw LoginException("TheAltening server is unavailable")
        }
        catch (exception: AuthenticationException)
        {
            throw LoginException(exception.message ?: "Unknown error")
        }
    }

    override fun toRawJson(json: JsonObject)
    {
        json["name"] = name
        json["token"] = token
    }

    override fun fromRawJson(json: JsonObject)
    {
        name = json.string("name")!!
        token = json.string("token")!!
    }
}
