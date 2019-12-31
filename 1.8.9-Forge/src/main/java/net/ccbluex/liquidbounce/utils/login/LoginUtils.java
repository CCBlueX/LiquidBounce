package net.ccbluex.liquidbounce.utils.login;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.SessionEvent;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.net.Proxy;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public final class LoginUtils extends MinecraftInstance {

    public static LoginResult login(final String username, final String password) {
        final YggdrasilUserAuthentication userAuthentication = (YggdrasilUserAuthentication)
                new YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);
        userAuthentication.setUsername(username);
        userAuthentication.setPassword(password);

        try {
            userAuthentication.logIn();

            mc.session = new Session(userAuthentication.getSelectedProfile().getName(),
                    userAuthentication.getSelectedProfile().getId().toString(), userAuthentication.getAuthenticatedToken(), "mojang");
            LiquidBounce.CLIENT.eventManager.callEvent(new SessionEvent());

            return LoginResult.LOGGED;
        }catch(AuthenticationUnavailableException exception) {
            return LoginResult.NO_CONTACT;
        }catch(AuthenticationException exception) {
            if(exception.getMessage().contains("Invalid username or password."))
                return LoginResult.INVALID_ACCOUNT_DATA;
            else if(exception.getMessage().toLowerCase().contains("account migrated"))
                return LoginResult.MIGRATED;
            else
                return LoginResult.NO_CONTACT;
        }catch(NullPointerException exception) {
            return LoginResult.WRONG_PASSWORD;
        }
    }

    public static void loginCracked(final String username) {
        mc.session = new Session(username, UserUtils.INSTANCE.getUUID(username), "-", "legacy");
        LiquidBounce.CLIENT.eventManager.callEvent(new SessionEvent());
    }

    public enum LoginResult {
        WRONG_PASSWORD,
        NO_CONTACT,
        INVALID_ACCOUNT_DATA,
        MIGRATED,
        LOGGED
    }
}
