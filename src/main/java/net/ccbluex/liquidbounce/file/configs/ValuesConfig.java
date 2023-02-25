/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat;
import net.ccbluex.liquidbounce.features.special.AutoReconnect;
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof;
import net.ccbluex.liquidbounce.features.special.ClientFixes;
import net.ccbluex.liquidbounce.file.FileConfig;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration;
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape;
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.value.Value;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

public class ValuesConfig extends FileConfig {

    /**
     * Constructor of config
     *
     * @param file of config
     */
    public ValuesConfig(final File file) {
        super(file);
    }

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Override
    protected void loadConfig() throws IOException {
        final JsonElement jsonElement = new JsonParser().parse(new BufferedReader(new FileReader(getFile())));

        if(jsonElement instanceof JsonNull)
            return;

        final JsonObject jsonObject = (JsonObject) jsonElement;

        final Iterator<Map.Entry<String, JsonElement>> iterator = jsonObject.entrySet().iterator();
        while(iterator.hasNext()) {
            final Map.Entry<String, JsonElement> entry = iterator.next();

            if (entry.getKey().equalsIgnoreCase("CommandPrefix")) {
                LiquidBounce.commandManager.setPrefix(entry.getValue().getAsCharacter());
            } else if (entry.getKey().equalsIgnoreCase("ShowRichPresence")) {
                LiquidBounce.clientRichPresence.setShowRichPresenceValue(entry.getValue().getAsBoolean());
            } else if (entry.getKey().equalsIgnoreCase("targets")) {
                JsonObject jsonValue = (JsonObject) entry.getValue();

                if (jsonValue.has("TargetPlayer"))
                    EntityUtils.targetPlayer = jsonValue.get("TargetPlayer").getAsBoolean();
                if (jsonValue.has("TargetMobs"))
                    EntityUtils.targetMobs = jsonValue.get("TargetMobs").getAsBoolean();
                if (jsonValue.has("TargetAnimals"))
                    EntityUtils.targetAnimals = jsonValue.get("TargetAnimals").getAsBoolean();
                if (jsonValue.has("TargetInvisible"))
                    EntityUtils.targetInvisible = jsonValue.get("TargetInvisible").getAsBoolean();
                if (jsonValue.has("TargetDead"))
                    EntityUtils.targetDead = jsonValue.get("TargetDead").getAsBoolean();
            } else if (entry.getKey().equalsIgnoreCase("features")) {
                JsonObject jsonValue = (JsonObject) entry.getValue();

                if (jsonValue.has("AntiForge"))
                    ClientFixes.fmlFixesEnabled = jsonValue.get("AntiForge").getAsBoolean();
                if (jsonValue.has("AntiForgeFML"))
                    ClientFixes.blockFML = jsonValue.get("AntiForgeFML").getAsBoolean();
                if (jsonValue.has("AntiForgeProxy"))
                    ClientFixes.blockProxyPacket = jsonValue.get("AntiForgeProxy").getAsBoolean();
                if (jsonValue.has("AntiForgePayloads"))
                    ClientFixes.blockPayloadPackets = jsonValue.get("AntiForgePayloads").getAsBoolean();
                if (jsonValue.has("FixResourcePackExploit"))
                    ClientFixes.blockResourcePackExploit = jsonValue.get("FixResourcePackExploit").getAsBoolean();
                if (jsonValue.has("ClientBrand"))
                    ClientFixes.clientBrand = jsonValue.get("ClientBrand").getAsString();
                if (jsonValue.has("BungeeSpoof"))
                    BungeeCordSpoof.enabled = jsonValue.get("BungeeSpoof").getAsBoolean();
                if (jsonValue.has("AutoReconnectDelay"))
                    AutoReconnect.INSTANCE.setDelay(jsonValue.get("AutoReconnectDelay").getAsInt());
            } else if (entry.getKey().equalsIgnoreCase("thealtening")) {
                JsonObject jsonValue = (JsonObject) entry.getValue();

                if (jsonValue.has("API-Key"))
                    GuiTheAltening.Companion.setApiKey(jsonValue.get("API-Key").getAsString());
            } else if (entry.getKey().equalsIgnoreCase("liquidchat")) {
                JsonObject jsonValue = (JsonObject) entry.getValue();

                if (jsonValue.has("token"))
                    LiquidChat.Companion.setJwtToken(jsonValue.get("token").getAsString());
            } else if (entry.getKey().equalsIgnoreCase("DonatorCape")) {
                JsonObject jsonValue = (JsonObject) entry.getValue();

                if (jsonValue.has("TransferCode"))
                    GuiDonatorCape.Companion.setTransferCode(jsonValue.get("TransferCode").getAsString());

                if (jsonValue.has("CapeEnabled"))
                    GuiDonatorCape.Companion.setCapeEnabled(jsonValue.get("CapeEnabled").getAsBoolean());
            } else if (entry.getKey().equalsIgnoreCase("clientConfiguration")) { // Compatibility with old versions
                JsonObject jsonValue = (JsonObject) entry.getValue();

                if (jsonValue.has("EnabledClientTitle"))
                    GuiClientConfiguration.Companion.setEnabledClientTitle(jsonValue.get("EnabledClientTitle").getAsBoolean());

                if (jsonValue.has("EnabledBackground"))
                    GuiClientConfiguration.Companion.setEnabledCustomBackground(jsonValue.get("EnabledBackground").getAsBoolean());

                if (jsonValue.has("Particles"))
                    GuiClientConfiguration.Companion.setParticles(jsonValue.get("Particles").getAsBoolean());
            } else if (entry.getKey().equalsIgnoreCase("Background")) {
                JsonObject jsonValue = (JsonObject) entry.getValue();

                if (jsonValue.has("Enabled"))
                    GuiClientConfiguration.Companion.setEnabledCustomBackground(jsonValue.get("Enabled").getAsBoolean());

                if (jsonValue.has("Particles"))
                    GuiClientConfiguration.Companion.setParticles(jsonValue.get("Particles").getAsBoolean());
            } else {
                final Module module = LiquidBounce.moduleManager.getModule(entry.getKey());

                if(module != null) {
                    final JsonObject jsonModule = (JsonObject) entry.getValue();

                    for(final Value moduleValue : module.getValues()) {
                        final JsonElement element = jsonModule.get(moduleValue.getName());

                        if(element != null) moduleValue.fromJson(element);
                    }
                }
            }
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Override
    protected void saveConfig() throws IOException {
        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("CommandPrefix", LiquidBounce.commandManager.getPrefix());
        jsonObject.addProperty("ShowRichPresence", LiquidBounce.clientRichPresence.getShowRichPresenceValue());

        final JsonObject jsonTargets = new JsonObject();
        jsonTargets.addProperty("TargetPlayer", EntityUtils.targetPlayer);
        jsonTargets.addProperty("TargetMobs", EntityUtils.targetMobs);
        jsonTargets.addProperty("TargetAnimals", EntityUtils.targetAnimals);
        jsonTargets.addProperty("TargetInvisible", EntityUtils.targetInvisible);
        jsonTargets.addProperty("TargetDead", EntityUtils.targetDead);
        jsonObject.add("targets", jsonTargets);

        final JsonObject jsonFeatures = new JsonObject();
        jsonFeatures.addProperty("AntiForge", ClientFixes.fmlFixesEnabled);
        jsonFeatures.addProperty("AntiForgeFML", ClientFixes.blockFML);
        jsonFeatures.addProperty("AntiForgeProxy", ClientFixes.blockProxyPacket);
        jsonFeatures.addProperty("AntiForgePayloads", ClientFixes.blockPayloadPackets);
        jsonFeatures.addProperty("FixResourcePackExploit", ClientFixes.blockResourcePackExploit);
        jsonFeatures.addProperty("ClientBrand", ClientFixes.clientBrand);
        jsonFeatures.addProperty("BungeeSpoof", BungeeCordSpoof.enabled);
        jsonFeatures.addProperty("AutoReconnectDelay", AutoReconnect.INSTANCE.getDelay());
        jsonObject.add("features", jsonFeatures);

        final JsonObject theAlteningObject = new JsonObject();
        theAlteningObject.addProperty("API-Key", GuiTheAltening.Companion.getApiKey());
        jsonObject.add("thealtening", theAlteningObject);

        final JsonObject liquidChatObject = new JsonObject();
        liquidChatObject.addProperty("token", LiquidChat.Companion.getJwtToken());
        jsonObject.add("liquidchat", liquidChatObject);

        final JsonObject capeObject = new JsonObject();
        capeObject.addProperty("TransferCode", GuiDonatorCape.Companion.getTransferCode());
        capeObject.addProperty("CapeEnabled", GuiDonatorCape.Companion.getCapeEnabled());
        jsonObject.add("DonatorCape", capeObject);

        final JsonObject clientObject = new JsonObject();
        clientObject.addProperty("EnabledClientTitle", GuiClientConfiguration.Companion.getEnabledClientTitle());
        clientObject.addProperty("EnabledBackground", GuiClientConfiguration.Companion.getEnabledCustomBackground());
        clientObject.addProperty("Particles", GuiClientConfiguration.Companion.getParticles());
        jsonObject.add("clientConfiguration", clientObject);

        LiquidBounce.moduleManager.getModules().stream().filter(module -> !module.getValues().isEmpty()).forEach(module -> {
            final JsonObject jsonModule = new JsonObject();
            module.getValues().forEach(value -> jsonModule.add(value.getName(), value.toJson()));
            jsonObject.add(module.getName(), jsonModule);
        });

        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        printWriter.println(FileManager.PRETTY_GSON.toJson(jsonObject));
        printWriter.close();
    }
}
