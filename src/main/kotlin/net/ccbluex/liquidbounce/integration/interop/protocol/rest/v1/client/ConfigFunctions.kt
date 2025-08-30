package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import kotlin.io.nameWithoutExtension


@Suppress("UNUSED_PARAMETER")
fun getConfigs(requestObject: RequestObject): FullHttpResponse {
    val folder = ConfigSystem.userConfigsFolder
    val configs = JsonArray().apply {
        folder.listFiles()?.forEach {
            add(it.nameWithoutExtension)
        }
    }

    return httpOk(configs)
}

fun loadConfig(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.queryParams["name"] ?: return httpForbidden("Name required")
    val file = ConfigSystem.userConfigsFolder.resolve("$name.json")
    if (!file.exists()) return httpForbidden("$name not found")

    file.bufferedReader().use { r ->
        AutoConfig.withLoading { AutoConfig.loadAutoConfig(r) }
    }
    return httpOk(JsonObject().apply { addProperty("loaded", name) })
}

fun saveConfig(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.queryParams["name"] ?: return httpForbidden("Name required")
    val file = ConfigSystem.userConfigsFolder.resolve("$name.json")
    file.createNewFile()
    file.bufferedWriter().use { AutoConfig.serializeAutoConfig(it) }
    return httpOk(JsonObject().apply { addProperty("saved", name) })
}

fun deleteConfig(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.queryParams["name"] ?: return httpForbidden("Name required")
    val file = ConfigSystem.userConfigsFolder.resolve("$name.json")
    if (!file.exists()) return httpForbidden("$name not found")
    file.delete()
    return httpOk(JsonObject().apply { addProperty("deleted", name) })
}
