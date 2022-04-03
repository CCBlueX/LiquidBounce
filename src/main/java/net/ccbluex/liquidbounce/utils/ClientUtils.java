/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.security.PublicKey;

@SideOnly(Side.CLIENT)
public final class ClientUtils extends MinecraftInstance {

    private static Field fastRenderField = null;

    static {
        try {
            final Field declaredField = GameSettings.class.getDeclaredField("ofFastRender");

            if (!declaredField.isAccessible()) {
                declaredField.setAccessible(true);
            }

            fastRenderField = declaredField;
        } catch (NoSuchFieldException e) { }
    }

    private static final Logger logger = LogManager.getLogger("LiquidBounce");

    public static Logger getLogger() {
        return logger;
    }

    public static void disableFastRender() {
        try {
            if (fastRenderField != null) {
                if (!fastRenderField.isAccessible()) {
                    fastRenderField.setAccessible(true);
                }

                fastRenderField.setBoolean(Minecraft.getMinecraft().gameSettings, false);
            }
        } catch (IllegalAccessException ignored) {
        }
    }

    public static void sendEncryption(final NetworkManager networkManager, final SecretKey secretKey, final PublicKey publicKey, final S01PacketEncryptionRequest encryptionRequest) {
        networkManager.sendPacket(new C01PacketEncryptionResponse(secretKey, publicKey, encryptionRequest.getVerifyToken()), future -> {
            networkManager.enableEncryption(secretKey);
        });
    }

    public static void displayChatMessage(final String message) {
        if (mc.thePlayer == null) {
            getLogger().info("(MCChat)" + message);
            return;
        }

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", message);

        mc.thePlayer.addChatMessage(IChatComponent.Serializer.jsonToComponent(jsonObject.toString()));
    }
}