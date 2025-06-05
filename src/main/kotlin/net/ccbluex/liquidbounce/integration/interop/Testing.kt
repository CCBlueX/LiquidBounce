package net.ccbluex.liquidbounce.integration.interop

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.gson.GsonConverter
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.util.json
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import kotlin.time.Duration.Companion.seconds

val interopServer = embeddedServer(Netty, port = 0) {
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
        // TODO: complete this
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("MyCustomHeader")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
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
        get("/") {
            call.respond(json {
                "name" to LiquidBounce.CLIENT_NAME
                "version" to LiquidBounce.clientVersion
                "author" to LiquidBounce.CLIENT_AUTHOR
            })
        }

        // Theme
        staticFiles("/", ThemeManager.themesFolder)

        // Event broadcasting
        webSocket("/") {
            // TODO
        }

        // REST
        route("/api/v1/client") {
            // TODO
        }
    }
}
