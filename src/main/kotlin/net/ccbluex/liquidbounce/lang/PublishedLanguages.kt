/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.lang

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.logger
import org.apache.commons.codec.digest.DigestUtils.sha256Hex
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Newer translations of this version, mirrored from this repository by CCBlueX/Translations, so fixes and
 * new languages reach a release without an update.
 *
 * [load] only reads the disk cache, so it is safe on the render thread; [download] fills it.
 */
internal object PublishedLanguages : LanguageManager.TranslationSource, EventListener {

    private const val HOST = "https://i18n.liquidbounce.net/liquidbounce"
    private const val NEXT = "next"

    // Minecraft reloads its resources when its language changes. Listening here keeps
    // LanguageManager out of the add-on ABI.
    @Suppress("unused")
    private val resourceReloadHandler = handler<ResourceReloadEvent> {
        withScope { LanguageManager.loadDefault() }
    }

    private class PublishedLanguage(val minecraft: String, val file: String, val sha256: String)

    private class Index(val languages: List<PublishedLanguage>)

    /**
     * The folder this client reads: its version's, or [NEXT] while that has not been released.
     */
    private class Channel(val name: String, val languages: List<PublishedLanguage>)

    @Volatile
    private var channel: Channel? = null

    @Volatile
    private var isFetched = false

    private fun folder(channel: String) = ConfigSystem.rootFolder.resolve("translations/$channel")

    private fun channel() = channel ?: cachedChannel().also { channel = it }

    private fun cachedChannel(): Channel {
        val name = LiquidBounce.clientVersion.takeIf { folder(it).resolve("index.json").isFile } ?: NEXT
        val languages = runCatching {
            folder(name).resolve("index.json").takeIf(File::isFile)?.let { parse(it.readText()) }
        }.onFailure { logger.error("Failed to read the cached translation index", it) }.getOrNull()

        return Channel(name, languages.orEmpty())
    }

    private fun parse(json: String) = json.readJson<Index>().languages.filter { language ->
        isPlainFileName(language.file).also { isPlain ->
            if (!isPlain) {
                logger.warn("Ignoring translation file ${language.file}")
            }
        }
    }

    private fun find(code: String) = channel().languages.firstOrNull { it.minecraft.equals(code, true) }

    val codes get() = channel().languages.map(PublishedLanguage::minecraft)

    fun has(code: String) = find(code) != null

    override fun load(code: String): Map<String, String>? {
        val language = find(code) ?: return null
        return folder(channel().name).resolve(language.file)
            .takeIf(File::isFile)
            ?.readJson<HashMap<String, String>>()
    }

    /**
     * Brings the cached file of [code] in line with the index, fetching the index first once per session.
     * Returns whether anything [load] reads has changed. Callers must not run it concurrently.
     */
    suspend fun download(code: String): Boolean = withContext(Dispatchers.IO) {
        val isIndexChanged = !isFetched && runCatching { fetchIndex() }
            .onFailure { logger.warn("Failed to fetch the translation index, using the cache", it) }
            .getOrDefault(false)

        val language = find(code) ?: return@withContext isIndexChanged
        val file = folder(channel().name).resolve(language.file)
        if (file.isFile && sha256Hex(file.readBytes()) == language.sha256) {
            return@withContext isIndexChanged
        }

        runCatching {
            val bytes = checkNotNull(fetch(channel().name, language.file)) { "${language.file} is not published" }
            check(sha256Hex(bytes) == language.sha256) { "${language.file} does not match its checksum" }
            write(file, bytes)
        }.onFailure { logger.warn("Failed to download the $code translation", it) }.isSuccess || isIndexChanged
    }

    private suspend fun fetchIndex(): Boolean {
        val version = LiquidBounce.clientVersion
        val published = if (version != "unofficial") fetch(version, "index.json") else null
        val name = if (published != null) version else NEXT
        val json = published ?: checkNotNull(fetch(NEXT, "index.json")) { "No translations are published" }
        val file = folder(name).resolve("index.json")
        val isChanged = name != channel().name || !file.isFile || !file.readBytes().contentEquals(json)

        channel = Channel(name, parse(json.decodeToString()))
        write(file, json)
        isFetched = true
        return isChanged
    }

    /**
     * Null when the host has no such file, which is how a version without its own folder shows.
     */
    private suspend fun fetch(channel: String, file: String): ByteArray? = try {
        HttpClient.request("$HOST/$channel/$file", HttpMethod.GET).use { it.body.bytes() }
    } catch (e: HttpException) {
        if (e.code == 404) null else throw e
    }

    private fun write(file: File, bytes: ByteArray) {
        file.parentFile.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(bytes)
        Files.move(temp.toPath(), file.toPath(), REPLACE_EXISTING, ATOMIC_MOVE)
    }

}

/**
 * The index names the files, so a name must not reach outside the cache folder.
 */
internal fun isPlainFileName(name: String) = name.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*"))
