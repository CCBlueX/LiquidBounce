package net.ccbluex.liquidbounce.integration.interop

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.GsonConverter
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveOrNull
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.util.collections.ConcurrentSet
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.channels.consumeEach
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.util.json
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.integration.theme.component.customComponents
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.minecraft.util.Util
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Properties
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds

private val wsSessions = ConcurrentSet<WebSocketSession>()

suspend fun broadcast(text: String) {
    wsSessions.forEach { session ->
        // TODO: reuse?
        session.send(Frame.Text(text))
    }
}

val interopServer = embeddedServer(Netty, port = 22493) {
    install(WebSockets) {
        // TODO: make it usable
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, GsonConverter(interopGson))
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.ContentLength)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Upgrade)
        allowHeader("X-Requested-With")

        allowOrigins {
            it == "localhost" || it == "127.0.0.1"
            // InetAddress.getByName(it).isLoopbackAddress
        }
    }

//    install(StatusPages) {
//        exception<FileNotFoundException> { call, _ ->
//            call.respond(HttpStatusCode.NotFound, "File not found")
//        }
//        exception<SecurityException> { call, _ ->
//            call.respond(HttpStatusCode.Forbidden, "Access denied")
//        }
//
////        exception<Throwable> { call, cause ->
////            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
////        }
//    }

    routing {
        // Root
        clientMetadata()

        // Theme
        themeFileTree()

        // Event broadcasting
        wsEventController()

        // REST
        route("/api/v1/client") {
            clientController()
            themeController()
            localStorageController()
            screenController()
            moduleController()
            componentController()
            accountController()
            proxyController()
            browserController()
            protocolController()
            spooferController()
            inputController()
            playerController()
            registryController()
            serverListController()
            textureController()
            worldController()
        }
    }
}

fun Route.clientMetadata() {
    get {
        call.respond(json {
            "name" to LiquidBounce.CLIENT_NAME
            "version" to LiquidBounce.clientVersion
            "author" to LiquidBounce.CLIENT_AUTHOR
        })
    }
}

fun Route.themeFileTree() {
    staticFiles("/", ThemeManager.themesFolder)
}

fun Route.wsEventController() {
    webSocket {
        wsSessions += this
        try {
            incoming.consumeEach {}
        } catch (e: Exception) {
            // TODO: logging?
        } finally {
            wsSessions -= this
        }
    }
}

fun Route.clientController() {
    get("/info") {
        call.respond(JsonObject().apply {
            addProperty("gameVersion", mc.gameVersion)
            addProperty("clientVersion", LiquidBounce.clientVersion)
            addProperty("clientName", LiquidBounce.CLIENT_NAME)
            addProperty("development", LiquidBounce.IN_DEVELOPMENT)
            addProperty("fps", mc.currentFps)
            addProperty("gameDir", mc.runDirectory.path)
            addProperty("inGame", inGame)
            addProperty("viaFabricPlus", usesViaFabricPlus)
            addProperty("hasProtocolHack", usesViaFabricPlus)
        })
    }
    get("/update") {
        call.respond(JsonObject().apply {
            addProperty("development", LiquidBounce.IN_DEVELOPMENT)
            addProperty("commit", LiquidBounce.clientCommit)

            val updateInfo = update ?: return@apply
            add("update", JsonObject().apply {
                addProperty("buildId", updateInfo.buildId)
                addProperty("commitId", updateInfo.commitId.substring(0, 7))
                addProperty("branch", updateInfo.branch)
                addProperty("clientVersion", updateInfo.lbVersion)
                addProperty("minecraftVersion", updateInfo.mcVersion)
                addProperty("release", updateInfo.release)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(updateInfo.date)
                addProperty("date", SimpleDateFormat().format(dateFormat))
                addProperty("message", updateInfo.message)

                addProperty("url", updateInfo.url)
            })
        })
    }
    post("/exit") {
        mc.scheduleStop()
        call.respond(HttpStatusCode.NoContent)
    }
    get("/window") {
        call.respond(JsonObject().apply {
            addProperty("width", mc.window.width)
            addProperty("height", mc.window.height)
            addProperty("scaledWidth", mc.window.scaledWidth)
            addProperty("scaledHeight", mc.window.scaledHeight)
            addProperty("scaleFactor", mc.window.scaleFactor)
            addProperty("guiScale", mc.options.guiScale.value)
        })
    }
    val POSSIBLE_URL_TARGETS: Map<String, URI> = with(Properties()) {
        LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/client_urls.properties").use {
            load(it)
        }

        stringPropertyNames().associateWith { URI(getProperty(it)) }
    }
    post("/browse") {
        class RequestBody(val target: String)
        val target = call.receiveNullable<RequestBody>()?.target ?: run {
            call.respond(HttpStatusCode.BadRequest, "No target specified")
            return@post
        }
        val url = POSSIBLE_URL_TARGETS[target] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Unknown target")
            return@post
        }
        Util.getOperatingSystem().open(url)
        call.respond(HttpStatusCode.NoContent)
    }
}

fun Route.themeController() {
    get("/theme") {
        call.respond(JsonObject().apply {
            addProperty("activeTheme", ThemeManager.activeTheme.name)
            addProperty("shaderEnabled", ThemeManager.shaderEnabled)
        })
    }
    post("/shader") {
        ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
        ConfigSystem.storeConfigurable(ThemeManager)
        call.respond(HttpStatusCode.NoContent)
    }
    get("/fonts") { // TODO: Unused?

    }
    get("/fonts/{name}") { // TODO: Unused?

    }
    get("/components") {
        call.respond(accessibleInteropGson.toJsonTree(components + customComponents))
    }
}

fun Route.localStorageController() {
    route("/localStorage") {
        get("/all") {
            call.respond(JsonObject().apply {
                val jsonArray = JsonArray()

                PersistentLocalStorage.forEach { (key, value) ->
                    jsonArray.add(JsonObject().apply {
                        addProperty("key", key)
                        addProperty("value", value)
                    })
                }

                add("items", jsonArray)
            })
        }
        put("/all") {
            data class Item(val key: String, val value: String)
            data class StoragePutRequest(val items: List<Item>)

            val body = call.receiveNullable<StoragePutRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid request")
                return@put
            }

            PersistentLocalStorage.clear()
            body.items.forEach { item ->
                PersistentLocalStorage[item.key] = item.value
            }

            call.respond(HttpStatusCode.NoContent)
        }
        get {
            val key = call.queryParameters["key"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "No key")
                return@get
            }
            val value = PersistentLocalStorage[key] ?: run {
                call.respond(HttpStatusCode.NotFound, "No value for key $key")
                return@get
            }
            call.respond(json { "value" to value })
        }
        put {
            class RequestBody(val key: String, val value: String)
            val body = call.receiveNullable<RequestBody>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "No key or value")
                return@put
            }

            PersistentLocalStorage[body.key] = body.value

            call.respond(HttpStatusCode.NoContent)
        }
        delete {
            val key = call.queryParameters["key"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "No key")
                return@delete
            }
            PersistentLocalStorage.remove(key)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.screenController() {

}

fun Route.moduleController() {

}

fun Route.componentController() {

}

fun Route.accountController() {

}

fun Route.proxyController() {}

fun Route.browserController() {}

fun Route.protocolController() {}

fun Route.spooferController() {}

fun Route.inputController() {}

fun Route.playerController() {}

fun Route.registryController() {}

fun Route.serverListController() {}

fun Route.textureController() {}

fun Route.worldController() {}
