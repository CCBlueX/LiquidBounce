package net.ccbluex.liquidbounce.discord

import com.google.gson.JsonParser
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.NetworkUtils
import org.json.JSONObject
import java.io.IOException
import java.time.OffsetDateTime

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class LiquidDiscordRPC : MinecraftInstance() {

    // IPC Client
    private var ipcClient: IPCClient? = null

    private var appID = 0L
    private val assets = mutableMapOf<String, String>()
    private val timestamp = OffsetDateTime.now()

    // Status of running
    private var running: Boolean = false

    /**
     * Setup Discord RPC
     */
    fun setup() {
        try {
            running = true

            loadConfiguration()

            ipcClient = IPCClient(appID)
            ipcClient?.setListener(object : IPCListener {

                /**
                 * Fired whenever an [IPCClient] is ready and connected to Discord.
                 *
                 * @param client The now ready IPCClient.
                 */
                override fun onReady(client: IPCClient?) {
                    Thread {
                        while (running) {
                            update()

                            try {
                                Thread.sleep(1000L)
                            } catch (ignored: InterruptedException) {
                            }

                        }
                    }.start()
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
            ipcClient?.connect()
        } catch (e: Throwable) {
            ClientUtils.getLogger().error("Failed to setup Discord RPC.", e)
        }

    }

    /**
     * Update rich presence
     */
    fun update() {
        val builder = RichPresence.Builder()

        // Set playing time
        builder.setStartTimestamp(timestamp)

        // Check assets contains logo and set logo
        if (assets.containsKey("logo"))
            builder.setLargeImage(assets["logo"], "MC ${LiquidBounce.MINECRAFT_VERSION} - ${LiquidBounce.CLIENT_NAME} b${LiquidBounce.CLIENT_VERSION}")

        // Check user is ingame
        if (mc.thePlayer != null) {
            val serverData = mc.currentServerData

            // Set display infos
            builder.setDetails("Server: ${if (mc.isIntegratedServerRunning || serverData == null) "Singleplayer" else serverData.serverIP}")
            builder.setState("Enabled ${LiquidBounce.moduleManager.modules.count { it.state }} of ${LiquidBounce.moduleManager.modules.size} modules")
        }

        // Check ipc client is connected and send rpc
        if (ipcClient?.status == PipeStatus.CONNECTED)
            ipcClient?.sendRichPresence(builder.build())
    }

    /**
     * Shutdown ipc client
     */
    fun shutdown() {
        try{
            ipcClient?.close()
        }catch(e: Throwable) {
            ClientUtils.getLogger().error("Failed to close Discord RPC.", e)
        }
    }

    /**
     * Load configuration from web
     *
     * @throws IOException If reading failed
     */
    private fun loadConfiguration() {
        // Read from web and convert to json object
        val jsonElement = JsonParser().parse(NetworkUtils.readContent("https://ccbluex.github.io/FileCloud/LiquidBounce/discord-rpc.json"))

        if (!jsonElement.isJsonObject)
            return

        val jsonObject = jsonElement.asJsonObject

        // Check has app id
        if (jsonObject.has("appID"))
            appID = jsonObject.get("appID").asLong

        // Import all asset names
        for ((key, value) in jsonObject.get("assets").asJsonObject.entrySet())
            assets[key] = value.asString
    }
}
