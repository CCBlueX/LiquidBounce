package net.ccbluex.liquidbounce;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ccbluex.liquidbounce.cape.CapeAPI;
import net.ccbluex.liquidbounce.discord.LiquidDiscordRPC;
import net.ccbluex.liquidbounce.event.ClientShutdownEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.features.command.CommandManager;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import net.ccbluex.liquidbounce.features.special.AntiForge;
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof;
import net.ccbluex.liquidbounce.features.special.DonatorCape;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.script.ScriptManager;
import net.ccbluex.liquidbounce.script.remapper.Remapper;
import net.ccbluex.liquidbounce.tabs.BlocksTab;
import net.ccbluex.liquidbounce.tabs.ExploitsTab;
import net.ccbluex.liquidbounce.tabs.HeadsTab;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui;
import net.ccbluex.liquidbounce.ui.client.hud.HUD;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClassUtils;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.misc.NetworkUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game Minecraft
 */
@SideOnly(Side.CLIENT)
public class LiquidBounce {

    // Instance of client
    public static final LiquidBounce CLIENT = new LiquidBounce();

    // Client information
    public static final String CLIENT_NAME = "LiquidBounce";
    public static final int CLIENT_VERSION = 69;
    public static final boolean IN_DEV = true;
    public static final String CLIENT_CREATOR = "CCBlueX";
    public static final String MINECRAFT_VERSION = "1.8.9";
    public boolean isStarting;

    // Managers
    public ModuleManager moduleManager;
    public CommandManager commandManager;
    public EventManager eventManager;
    public FileManager fileManager;
    public ScriptManager scriptManager;

    // HUD & ClickGUI
    public HUD hud;
    public ClickGui clickGui;

    // Update information
    public int latestVersion;

    // Menu Background
    public ResourceLocation background;

    // Discord RPC
    private LiquidDiscordRPC liquidDiscordRPC;

    private LiquidBounce() {
    }

    /**
     * Execute if client will be started
     */
    public void startClient() {
        // Client start
        isStarting = true;
        ClientUtils.getLogger().info("Starting " + CLIENT_NAME + " b" + CLIENT_VERSION + ", by " + CLIENT_CREATOR);

        // Create file manager
        fileManager = new FileManager();

        // Crate event manager
        eventManager = new EventManager();

        // Register rotation listener
        eventManager.registerListener(new RotationUtils());

        // Register anti forge listener
        eventManager.registerListener(new AntiForge());

        // Register BungeeCord spoof listener
        eventManager.registerListener(new BungeeCordSpoof());

        // Register donator cape mover listener
        eventManager.registerListener(new DonatorCape());

        // Create CommandManager
        commandManager = new CommandManager();

        // Load fonts
        Fonts.loadFonts();

        // Setup module manager and register modules
        moduleManager = ModuleManager.INSTANCE;
        moduleManager.registerModules();

        // Remapper
        try {
            Remapper.INSTANCE.loadSrg();

            // ScriptManager
            scriptManager = new ScriptManager();
            scriptManager.loadScripts();
            scriptManager.enableScripts();
        } catch (final Throwable throwable) {
            ClientUtils.getLogger().error("Failed to load scripts.", throwable);
        }

        // Register commands
        commandManager.registerCommands();

        // Load configs
        fileManager.loadConfigs(fileManager.modulesConfig, fileManager.valuesConfig, fileManager.accountsConfig,
                fileManager.friendsConfig, fileManager.xrayConfig, fileManager.shortcutsConfig);

        // ClickGUI
        clickGui = new ClickGui();
        fileManager.loadConfig(fileManager.clickGuiConfig);

        // Tabs
        // Check if minecraft is forge
        if (ClassUtils.hasClass("net.minecraftforge.common.MinecraftForge")) {
            // Register tabs
            new BlocksTab();
            new ExploitsTab();
            new HeadsTab();
        }

        // Register capes service
        try {
            CapeAPI.INSTANCE.registerCapeService();
        } catch (final Throwable throwable) {
            ClientUtils.getLogger().error("Failed to register cape service", throwable);
        }

        // Setup Discord RPC
        try {
            (liquidDiscordRPC = new LiquidDiscordRPC()).setup();
        } catch (final Throwable throwable) {
            ClientUtils.getLogger().error("Failed to setup Discord RPC.", throwable);
        }

        // Set HUD
        hud = HUD.createDefault();
        fileManager.loadConfig(fileManager.hudConfig);

        // Disable fastrender
        ClientUtils.disableFastRender();

        try {
            // Read versions json from cloud
            final JsonElement jsonElement = new JsonParser().parse(NetworkUtils.readContent("https://ccbluex.github.io/FileCloud/LiquidBounce/versions.json"));

            // Check json is valid object
            if (jsonElement.isJsonObject()) {
                // Get json object of element
                final JsonObject jsonObject = jsonElement.getAsJsonObject();

                // Check has minecraft version
                if (jsonObject.has(MINECRAFT_VERSION)) {
                    // Get client version
                    latestVersion = jsonObject.get(MINECRAFT_VERSION).getAsInt();
                }
            }
        } catch (final Throwable exception) {
            // Print throwable to console
            ClientUtils.getLogger().error("Failed to check for updates.", exception);
        }

        // Load generators
        GuiAltManager.loadGenerators();

        // Set is starting status
        isStarting = false;
    }

    /**
     * Execute if client will be stopped
     */
    public void stopClient() {
        eventManager.callEvent(new ClientShutdownEvent());

        // Check if filemanager is available
        if (fileManager != null) {
            // Save all configs of file manager
            fileManager.saveAllConfigs();
        }

        // Check if discord rpc is available
        if (liquidDiscordRPC != null)
            // Shutdown discord rpc
            liquidDiscordRPC.shutdown();
    }
}
