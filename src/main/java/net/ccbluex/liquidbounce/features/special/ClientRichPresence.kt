/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.special

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.MINECRAFT_VERSION
import net.ccbluex.liquidbounce.LiquidBounce.clientCommit
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import org.json.JSONObject
import java.io.IOException
import java.time.OffsetDateTime
import kotlin.concurrent.thread

object ClientRichPresence : MinecraftInstance() {

    var showRPCValue = true
    var showRPCServerIP = true
    var customRPCText = ""

    // IPC Client
    private var ipcClient: IPCClient? = null

    private var appID = 0L
    private val assets = mutableMapOf<String, String>()
    private val timestamp = OffsetDateTime.now()

    // Status of running
    private var running = false

    /**
     * Setup Discord RPC
     */
    fun setup() {
        try {
            running = true

            loadConfiguration()

            ipcClient = IPCClient(appID).apply {
                setListener(object : IPCListener {

                    /**
                     * Fired whenever an [IPCClient] is ready and connected to Discord.
                     *
                     * @param client The now ready IPCClient.
                     */
                    override fun onReady(client: IPCClient?) {
                        thread {
                            while (running) {
                                update()
                                Thread.sleep(1000L)
                            }
                        }
                    }

                    /**
                     * Fired whenever an [IPCClient] has closed.
                     *
                     * @param client The now closed IPCClient.
                     * @param json A [JSONObject] with close data.
                     */
                    override fun onClose(client: IPCClient?, json: JSONObject?) {
                        running = false
                    }

                })
            }
            ipcClient?.connect()
        } catch (e: Throwable) {
            LOGGER.error("Failed to setup Discord RPC.", e)
        }

    }

    /**
     * Update rich presence
     */
    fun update() {
        if (ipcClient?.status != PipeStatus.CONNECTED) return

        val builder = RichPresence.Builder().apply {
            // Set playing time
            setStartTimestamp(timestamp)

            // Check assets contains logo and set logo
            assets["logo"]?.let {
                setLargeImage(it, "MC $MINECRAFT_VERSION - $CLIENT_NAME $clientVersionText $clientCommit")
            }

            // Check user is in-game
            mc.thePlayer?.let {
                val serverData = mc.currentServerData

                // Set server info
                if (showRPCServerIP) {
                    setDetails(customRPCText.ifEmpty {
                        "Server: ${if (mc.isIntegratedServerRunning || serverData == null) "Singleplayer" else ServerUtils.hideSensitiveInformation(serverData.serverIP)}"
                    })
                }

                // Set modules info
                setState("Enabled ${moduleManager.modules.count { it.state }} of ${moduleManager.modules.size} modules")
            }
        }

        // Check ipc client is connected and send rpc
        if (ipcClient?.status == PipeStatus.CONNECTED)
            ipcClient?.sendRichPresence(builder.build())
    }

    /**
     * Shutdown ipc client
     */
    fun shutdown() {
        if (ipcClient?.status != PipeStatus.CONNECTED) {
            return
        }

        try {
            ipcClient?.close()
        } catch (e: Throwable) {
            LOGGER.error("Failed to close Discord RPC.", e)
        }
    }

    /**
     * Load configuration from web
     *
     * @throws IOException If reading failed
     */
    private fun loadConfiguration() {
        val (response, _) = get("$CLIENT_CLOUD/discord.json")

        // Read from web and convert to json object
        val json = JsonParser().parse(response)

        if (json !is JsonObject)
            return

        // Check has app id
        if (json.has("appID"))
            appID = json["appID"].asLong

        // Import all asset names
        for ((key, value) in json["assets"].asJsonObject.entrySet())
            assets[key] = value.asString
    }
}
