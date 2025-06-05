@file:Suppress("detekt:ALL")
package net.ccbluex.liquidbounce.integration.interop

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
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
import io.ktor.server.request.contentCharset
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondSource
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
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
import kotlinx.coroutines.future.await
import kotlinx.io.asByteChannel
import kotlinx.io.asOutputStream
import kotlinx.io.asSource
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ResourcePolicy
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.config.gson.util.json
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfigurable
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.VirtualDisplayScreen
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ACCEPTED_BLOCK_TAGS
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ACCEPTED_ITEM_TAGS
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList.pingThemAll
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList.serverList
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerInventoryData
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.constructMap
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.getByAddress
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.isTyping
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.servers
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.integration.theme.component.customComponents
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.ccbluex.netty.http.util.readImageAsBase64
import net.minecraft.client.gui.screen.SplashOverlay
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.world.EditWorldScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.screen.world.SymlinkWarningScreen
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.network.ServerInfo.ResourcePackPolicy
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.toast.SystemToast
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.client.util.InputUtil
import net.minecraft.item.BlockItem
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.util.path.SymlinkValidationException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Properties
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.seconds

private val wsSessions = ConcurrentSet<WebSocketSession>()

suspend fun RoutingCall.receiveReader() =
    receiveStream().reader(request.contentCharset() ?: Charsets.UTF_8)

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

    // TODO: ContentTransformationException -> BadRequest
    // Exceptions
    // - JSON body parse failed
    // - missing query param
    // - default Throwable


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
            sessionController()
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
    get("/fonts") {
        call.respond(FontManager.fontFaces.map { it.name })
    }
    get("/fonts/{name}") { // TODO: Unused?
        val name = call.parameters["name"] ?: ""
        val font = FontManager.fontFace(name) ?: run {
            call.respond(HttpStatusCode.NotFound, "No font named $name")
            return@get
        }
        if (font.file != null) {
            call.respondFile(font.file)
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
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
    get("/virtualScreen") {
        call.respond(JsonObject().apply {
            addProperty("name", IntegrationListener.momentaryVirtualScreen?.type?.routeName)
            addProperty("showingSplash", mc.overlay is SplashOverlay)
        })
    }
    post("/virtualScreen") { // TODO: unused?

    }
    route("/screen") {
        get {
            val mcScreen = mc.currentScreen ?: run {
                call.respond(HttpStatusCode.TemporaryRedirect, "No screen")
                return@get
            }
            val name = VirtualScreenType.recognize(mcScreen)?.routeName ?: mcScreen.javaClass.name
            call.respond(json { "name" to name })
        }
        get("/size") {
            call.respond(JsonObject().apply {
                addProperty("width", mc.window.scaledWidth)
                addProperty("height", mc.window.scaledHeight)
            })
        }
        put {
            class RequestBody(val name: String)
            val screenName = call.receiveNullable<RequestBody>()?.name ?: run {
                call.respond(HttpStatusCode.BadRequest, "No screen name")
                return@put
            }

            VirtualScreenType.byName(screenName)?.open() ?: run {
                call.respond(HttpStatusCode.NotFound, "Screen name $screenName not found")
                return@put
            }

            call.respond(HttpStatusCode.NoContent)
        }
        delete {
            val screen = mc.currentScreen ?: run {
                call.respond(HttpStatusCode.TemporaryRedirect, "No screen")
                return@delete
            }
            if (screen is VirtualDisplayScreen && screen.parentScreen != null) {
                RenderSystem.recordRenderCall {
                    mc.setScreen(screen.parentScreen)
                }
            } else {
                RenderSystem.recordRenderCall {
                    mc.setScreen(if (inGame) null else TitleScreen())
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.moduleController() {
    fun ClientModule.toJsonObject() = JsonObject().apply {
        addProperty("name", name)
        addProperty("category", category.readableName)
        add("keyBind", interopGson.toJsonTree(bind))
        addProperty("enabled", enabled)
        addProperty("description", description.get())
        addProperty("tag", tag)
        addProperty("hidden", hidden)
        add("aliases", interopGson.toJsonTree(aliases))
    }

    suspend fun RoutingContext.handleModuleName(name: String?): ClientModule? {
        if (name == null) {
            call.respond(HttpStatusCode.BadRequest, "No module name")
            return@handleModuleName null
        }
        val module = ModuleManager[name] ?: run {
            call.respond(HttpStatusCode.NotFound, "Module '$name' not found")
            return@handleModuleName null
        }
        return module
    }

    get("/module/{name}") {
        val module = handleModuleName(call.parameters["name"]) ?: return@get
        call.respond(module.toJsonObject())
    }
    route("/modules") {
        get {
            call.respond(ModuleManager.map { it.toJsonObject() })
        }
        route("/toggle") {
            handle {
                class RequestBody(val name: String)
                val module = handleModuleName(call.receiveNullable<RequestBody>()?.name) ?: return@handle

                val supposedNew = when (call.request.httpMethod) {
                    HttpMethod.Put -> true
                    HttpMethod.Delete -> false
                    HttpMethod.Post -> !module.enabled
                    else -> {
                        call.respond(HttpStatusCode.MethodNotAllowed)
                        return@handle
                    }
                }

                if (module.enabled == supposedNew) {
                    call.respond(HttpStatusCode.Forbidden, "${module.name} already ${if (supposedNew) "enabled" else "disabled"}")
                    return@handle
                }

                RenderSystem.recordRenderCall {
                    runCatching {
                        module.enabled = supposedNew

                        ConfigSystem.storeConfigurable(modulesConfigurable)
                    }.onFailure {
                        logger.error("Failed to toggle module ${module.name}", it)
                    }
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
        get("/settings") {
            val module = handleModuleName(call.queryParameters["name"]) ?: return@get
            call.respond(ConfigSystem.serializeConfigurable(module, gson = interopGson))
        }
        put("/settings") {
            val module = handleModuleName(call.queryParameters["name"]) ?: return@put

            ConfigSystem.deserializeConfigurable(module, call.receiveReader())
            ConfigSystem.storeConfigurable(modulesConfigurable)

            call.respond(HttpStatusCode.NoContent)
        }
        post("/panic") {
            RenderSystem.recordRenderCall {
                AutoConfig.withLoading {
                    runCatching {
                        for (module in ModuleManager) {
                            if (module.category == Category.RENDER || module.category == Category.CLIENT) {
                                continue
                            }

                            module.enabled = false
                        }

                        ConfigSystem.storeConfigurable(modulesConfigurable)
                    }.onFailure {
                        logger.error("Failed to panic disable modules", it)
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.sessionController() {
    get("/session") {
        val session = mc.session ?: call.respond(HttpStatusCode.NotFound, "No session")
        call.respond(session)
    }
    get("/location") {
        val locationInfo = IpInfoApi.current ?: run {
            call.respond(HttpStatusCode.NotFound, "Unknown location")
            return@get
        }
        call.respond(locationInfo)
    }
}

fun Route.accountController() {
    TODO()
}

fun Route.proxyController() {
    TODO()
}

fun Route.browserController() {
    TODO()
}

fun Route.protocolController() {
    TODO()
}

fun Route.spooferController() {
    route("/spoofer") {
        get {
            call.respond(ConfigSystem.serializeConfigurable(SpooferManager, gson = interopGson))
        }
        put {
            ConfigSystem.deserializeConfigurable(SpooferManager, call.receiveReader())
            ConfigSystem.storeConfigurable(SpooferManager)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.inputController() {
    get("/input") {
        val key = call.queryParameters["key"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "No key")
            return@get
        }
        val input = InputUtil.fromTranslationKey(key)
        call.respond(JsonObject().apply {
            addProperty("translationKey", input.translationKey)
            addProperty("localized", input.localizedText.convertToString())
        })
    }
    get("/keybinds") {
        call.respond(
            mc.options.allKeys.map { key ->
                JsonObject().apply {
                    addProperty("bindName", key.translationKey)
                    add("key", JsonObject().apply {
                        addProperty("translationKey", key.boundKeyTranslationKey)
                        addProperty("localized", key.boundKeyLocalizedText?.convertToString())
                    })
                }
            }
        )
    }
    get("/typing") {
        call.respond(json { "typing" to isTyping })
    }
    post("/typing") {
        class TypingRequest(val typing: Boolean)

        val typingRequest = call.receive<TypingRequest>()
        isTyping = typingRequest.typing

        call.respond(HttpStatusCode.NoContent)
    }
}

fun Route.playerController() {
    route("/player") {
        get {
            call.respond(PlayerData.fromPlayer(player))
        }
        get("/inventory") {
            call.respond(PlayerInventoryData.fromPlayer(player))
        }
    }
    get("/crosshair") {
        mc.crosshairTarget?.let {
            call.respond(it)
        } ?: call.respond(HttpStatusCode.NoContent)
    }
}

fun Route.registryController() {
    get("/registories") {
        call.respond(JsonObject().apply {
            val world = mc.world ?: run {
                call.respond(HttpStatusCode.TemporaryRedirect, "No world")
                return@get
            }

            val parentMap = hashMapOf<Identifier, Identifier>()

            Registries.BLOCK.forEach {
                val pickStack = it.getPickStack(world, BlockPos.ORIGIN, it.defaultState, false)
                val id = Registries.BLOCK.getId(it)

                when (val item = pickStack.item) {
                    is BlockItem -> {
                        if (item.block != it) {
                            parentMap[id] = Registries.BLOCK.getId(item.block)
                        }
                    }
                    else -> {
                        if (!pickStack.isNothing()) {
                            logger.warn("Invalid pick stack for $id: $pickStack")
                        }
                    }
                }
            }

            add("blocks", JsonArray().apply {
                Registries.BLOCK.forEach { block ->
                    val jsonObject = JsonObject().apply {
                        addProperty("identifier", Registries.BLOCK.getId(block).toString())
                        addProperty("name", block.name.convertToString())
                    }
                    add(jsonObject)
                }
            })
            add("items", JsonArray().apply {
                Registries.ITEM.forEach { item ->
                    val jsonObject = JsonObject().apply {
                        addProperty("identifier", Registries.ITEM.getId(item).toString())
                        addProperty("name", item.name.convertToString())
                    }
                    add(jsonObject)
                }
            })
            add("itemGroups", JsonObject().apply {
                for ((k, v) in constructMap(Registries.ITEM, ACCEPTED_ITEM_TAGS)) {
                    add(
                        k.toString(),
                        JsonObject().apply {
                            addProperty("relation", "group")
                            addProperty("relative", v.toString())
                        }
                    )
                }
            })
            add("blockGroups", JsonObject().apply {
                val constructedMap = constructMap(Registries.BLOCK, ACCEPTED_BLOCK_TAGS)

                Registries.BLOCK.forEach { block ->
                    val id = Registries.BLOCK.getId(block)

                    val obj = when (id) {
                        in parentMap -> JsonObject().apply {
                            addProperty("relation", "parent")
                            addProperty("relative", parentMap[id]!!.toString())
                        }

                        in constructedMap -> JsonObject().apply {
                            addProperty("relation", "group")
                            addProperty("relative", constructedMap[id]!!.toString())
                        }

                        else -> return@forEach
                    }

                    add(id.toString(), obj)
                }
            })
        })
    }
}

fun Route.serverListController() {
    route("/servers") {
        get {
            try {
                serverList.loadFile()
                pingThemAll()

                val servers = JsonArray()
                serverList.servers.forEachIndexed { id, serverInfo ->
                    val json = interopGson.toJsonTree(serverInfo)

                    if (!json.isJsonObject) {
                        logger.warn("Failed to convert serverInfo to json")
                        return@forEachIndexed
                    }

                    val jsonObject = json.asJsonObject
                    jsonObject.addProperty("id", id)
                    servers.add(jsonObject)
                }

                call.respond(servers)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to get servers due to ${e.message}")
            }
        }
        put("/add") {
            class ServerAddRequest(val name: String, val address: String, val resourcePackPolicy: String? = null)
            val serverAddRequest = call.receiveNullable<ServerAddRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                return@put
            }

            if (!ServerAddress.isValid(serverAddRequest.address)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid address")
                return@put
            }

            val serverInfo = ServerInfo(serverAddRequest.name, serverAddRequest.address, ServerInfo.ServerType.OTHER)
            serverAddRequest.resourcePackPolicy?.let {
                serverInfo.resourcePackPolicy = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ResourcePackPolicy.PROMPT
            }

            serverList.add(serverInfo, false)
            serverList.saveFile()

            call.respond(HttpStatusCode.NoContent)
        }
        delete("/remove") {
            class ServerRemoveRequest(val id: Int)
            val serverRemoveRequest = call.receiveNullable<ServerRemoveRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "No id")
                return@delete
            }
            val serverInfo = serverList.get(serverRemoveRequest.id)

            serverList.remove(serverInfo)
            serverList.saveFile()

            call.respond(HttpStatusCode.NoContent)
        }
        put("/edit") {
            class ServerEditRequest(
                val id: Int,
                val name: String,
                val address: String,
                val resourcePackPolicy: String? = null
            )
            val serverEditRequest = call.receiveNullable<ServerEditRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                return@put
            }
            val serverInfo = serverList.get(serverEditRequest.id)

            serverInfo.name = serverEditRequest.name
            serverInfo.address = serverEditRequest.address
            serverEditRequest.resourcePackPolicy?.let {
                serverInfo.resourcePackPolicy = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ResourcePackPolicy.PROMPT
            }
            serverList.saveFile()

            call.respond(HttpStatusCode.NoContent)
        }
        post("/swap") {
            class ServerSwapRequest(val from: Int, val to: Int)
            val serverSwapRequest = call.receiveNullable<ServerSwapRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                return@post
            }

            serverList.swapEntries(serverSwapRequest.from, serverSwapRequest.to)
            serverList.saveFile()

            call.respond(HttpStatusCode.NoContent)
        }
        post("/order") {
            class ServerOrderRequest(val order: List<Int>)
            val serverOrderRequest = call.receiveNullable<ServerOrderRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                return@post
            }

            serverOrderRequest.order.map { serverList.get(it) }
                .forEachIndexed { index, serverInfo ->
                    serverList.set(index, serverInfo)
                }
            serverList.saveFile()

            call.respond(HttpStatusCode.NoContent)
        }
        post("/connect") {
            class ServerConnectRequest(val address: String)
            val serverConnectRequest = call.receiveNullable<ServerConnectRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "No address")
                return@post
            }
            val serverInfo = serverList.getByAddress(serverConnectRequest.address)
                ?: ServerInfo("Unknown Server", serverConnectRequest.address, ServerInfo.ServerType.OTHER)

            val serverAddress = ServerAddress.parse(serverInfo.address)

            RenderSystem.recordRenderCall {
                ConnectScreen.connect(MultiplayerScreen(TitleScreen()), mc, serverAddress, serverInfo, false, null)
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.textureController() {
    suspend fun RoutingContext.handleId(id: String?): Identifier? {
        val identifier = call.queryParameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "No identifier")
            return@handleId null
        }

        return try {
            Identifier.of(identifier)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.NotFound, "Invalid identifier")
            null
        }
    }

    route("/resource") {
        get {
            val identifier = handleId(call.queryParameters["id"]) ?: return@get
            val resource = try {
                mc.resourceManager.getResourceOrThrow(identifier)
            } catch (e: FileNotFoundException) {
                call.respond(HttpStatusCode.NotFound, "File not found")
                return@get
            }

            call.respondSource(resource.inputStream.asSource(), contentType = ContentType.Application.OctetStream)
        }
        get("/itemTexture") {
            if (!ItemImageAtlas.isAtlasAvailable) {
                call.respond(HttpStatusCode.InternalServerError, "Item atlas not available yet")
                return@get
            }

            val identifier = handleId(call.queryParameters["id"]) ?: return@get

            val alternativeIdentifier = ItemImageAtlas.resolveAliasIfPresent(identifier)

            val of = RegistryKey.of(RegistryKeys.ITEM, alternativeIdentifier)

            val image = Registries.ITEM.get(of)?.let(ItemImageAtlas::getItemImage) ?: run {
                call.respond(HttpStatusCode.NotFound, "Item image not found")
                return@get
            }

            val buffer = kotlinx.io.Buffer()

            ImageIO.write(image, "PNG", buffer.asOutputStream())
            call.respondSource(buffer, contentType = ContentType.Image.PNG, contentLength = buffer.size)
        }
        get("/skin") {
            val uuid = try {
                call.queryParameters["uuid"]?.let(UUID::fromString) ?: run {
                    call.respond(HttpStatusCode.BadRequest, "No UUID")
                    return@get
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid UUID parameter")
                return@get
            }

            val skinTextures = world.players.find { it.uuid == uuid }?.skinTextures
                ?: DefaultSkinHelper.getSkinTextures(uuid)
            val texture = mc.textureManager.getTexture(skinTextures.texture)

            if (texture is NativeImageBackedTexture) {
                val buffer = kotlinx.io.Buffer()

                texture.image?.write(buffer.asByteChannel()) ?: run {
                    call.respond(HttpStatusCode.InternalServerError, "Texture is not cached yet")
                    return@get
                }

                call.respondSource(buffer, contentType = ContentType.Image.PNG, contentLength = buffer.size)
            } else {
                val resource = mc.resourceManager.getResource(skinTextures.texture)
                    .getOrNull() ?: run {
                        call.respond(HttpStatusCode.InternalServerError, "Texture not found")
                        return@get
                    }

                call.respondSource(resource.inputStream.asSource(), contentType = ContentType.Image.PNG)
            }
        }
    }
}

fun Route.worldController() {
    route("/worlds") {
        class LevelRequest(val name: String)
        get {
            try {
                val levelList = mc.levelStorage.levelList
                if (levelList.isEmpty) {
                    call.respond(emptyList<Nothing>())
                    return@get
                }

                val summaries = mc.levelStorage.loadSummaries(levelList).await()

                val worlds = summaries.mapIndexed { index, summary ->
                    JsonObject().apply {
                        addProperty("id", index)
                        addProperty("name", summary.name)
                        addProperty("displayName", summary.displayName)
                        addProperty("lastPlayed", summary.lastPlayed)
                        addProperty("gameMode", summary.levelInfo.gameMode.getName())
                        addProperty("difficulty", summary.levelInfo.difficulty.getName())
                        addProperty("icon", runCatching { readImageAsBase64(summary.iconPath) }.onFailure {
                            //logger.error("Failed to read icon for world ${summary.name}", it)
                        }.getOrNull())
                        addProperty("version", summary.versionInfo.versionName)
                        addProperty("hardcore", summary.levelInfo.isHardcore)
                        addProperty("commandsAllowed", summary.levelInfo.areCommandsAllowed())
                        addProperty("locked", summary.isLocked)
                        addProperty("requiresConversion", summary.requiresConversion())
                        addProperty("isVersionAvailable", summary.isVersionAvailable)
                        addProperty("shouldPromptBackup", summary.shouldPromptBackup())
                        addProperty("wouldBeDowngraded", summary.wouldBeDowngraded())
                    }
                }

                call.respond(worlds)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to get worlds due to ${e.message}")
            }
        }
        post("/join") {
            val request = call.receiveNullable<LevelRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "No world name")
                return@post
            }

            RenderSystem.recordRenderCall {
                runCatching {
                    mc.createIntegratedServerLoader().start(request.name) {
                        mc.setScreen(SelectWorldScreen(TitleScreen()))
                    }
                }.onFailure {
                    logger.error("Failed to join world ${request.name}", it)
                }
            }

            call.respond(HttpStatusCode.NoContent)
        }
        post("/edit") {
            val request = call.receiveNullable<LevelRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "No world name")
                return@post
            }

            RenderSystem.recordRenderCall {
                val session = runCatching {
                    mc.levelStorage.createSession(request.name)
                }.onFailure { exception ->
                    when (exception) {
                        is IOException -> {
                            SystemToast.addWorldAccessFailureToast(mc, request.name)
                            logger.error("Failed to access level ${request.name}", exception)
                        }
                        is SymlinkValidationException -> {
                            logger.warn(exception.message)
                            mc.setScreen(SymlinkWarningScreen.world { mc.setScreen(SelectWorldScreen(TitleScreen())) })
                        }
                        else -> {
                            logger.error("Failed to access level ${request.name}", exception)
                        }
                    }
                }.getOrNull() ?: return@recordRenderCall

                runCatching {
                    EditWorldScreen.create(mc, session) { _ ->
                        session.tryClose()
                        mc.setScreen(SelectWorldScreen(TitleScreen()))
                    }
                }.onFailure { exception ->
                    session.tryClose()
                    SystemToast.addWorldAccessFailureToast(mc, request.name)
                    logger.error("Failed to load world data ${request.name}", exception)
                }.onSuccess { screen ->
                    mc.setScreen(screen)
                }
            }

            call.respond(HttpStatusCode.NoContent)
        }
        post("/delete") {
            val request = call.receiveNullable<LevelRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "No world name")
                return@post
            }

            runCatching {
                mc.levelStorage.createSessionWithoutSymlinkCheck(request.name).use { session ->
                    session.deleteSessionLock()
                }
                call.respond(HttpStatusCode.NoContent)
            }.onFailure {
                logger.error("Failed to delete world ${request.name}", it)
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "")
            }
        }
    }
}
