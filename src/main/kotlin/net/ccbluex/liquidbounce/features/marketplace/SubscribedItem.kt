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

package net.ccbluex.liquidbounce.features.marketplace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient.request
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.addon.AddonInstaller
import net.ccbluex.liquidbounce.integration.task.type.ResourceTask
import net.ccbluex.liquidbounce.mcef.listeners.OkHttpProgressInterceptor
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val itemLocks = ConcurrentHashMap<Int, Mutex>()

private const val RETIRED_SUFFIX = ".old"
private val retiredName = Regex("""\.\d+\.old""")

private const val RENAME_ATTEMPTS = 20
private const val RENAME_RETRY_DELAY_MS = 100L

private fun contentFolder(revisionDir: File): File? {
    fun File.containsFile(): Boolean {
        return this.isDirectory && !this.listFiles(File::isFile).isNullOrEmpty()
    }

    if (revisionDir.containsFile()) {
        return revisionDir
    }

    // Return null if no files found at folder or one level below
    return revisionDir.listFiles(File::isDirectory)?.firstOrNull { subFolder ->
        subFolder.containsFile()
    }
}

// Windows refuses to rename a directory while a virus scanner still reads a jar just extracted into it.
private suspend fun File.rename(target: File) {
    var attempts = 1
    while (!renameTo(target)) {
        check(attempts++ < RENAME_ATTEMPTS) { "Failed to rename $name to ${target.name}" }
        delay(RENAME_RETRY_DELAY_MS)
    }
}

@AddonApi
@Suppress("TooManyFunctions")
data class SubscribedItem(val name: String, val id: Int, val type: MarketplaceItemType) {

    constructor(item: MarketplaceItem) : this(item.name, item.id, item.type) {
        require(item.type.isSubscribable) { "Type ${item.type} is not subscribable" }
        author = item.author
    }

    /**
     * Tells apart subscriptions that share a name. Unknown for ones saved before it was kept, until
     * [MarketplaceManager.fillAuthors] looks it up.
     */
    internal var author: String? = null

    val itemDir
        get() = MarketplaceManager.marketplaceRoot.resolve("items/$id")

    /**
     * The revision currently unpacked in [itemDir], or `null` when nothing is installed.
     */
    val installedRevisionId: Int?
        get() = installedRevisionDir?.name?.toInt()

    /**
     * The revision directory currently unpacked in [itemDir], or `null` when nothing is installed.
     * Installing leaves exactly one numeric directory; anything in the making is named otherwise.
     */
    internal val installedRevisionDir: File?
        get() = revisionDirs.maxByOrNull { it.name.toInt() }

    private val revisionDirs: List<File>
        get() = itemDir.listFiles { file: File -> file.isDirectory && file.name.toIntOrNull() != null }
            ?.asList().orEmpty()

    private val lock
        get() = itemLocks.computeIfAbsent(id) { Mutex() }

    /**
     * Get the installation folder of the item.
     *
     * Walks down the revision folder until it finds a file,
     * which returns the parent folder of that file,
     * as the installation folder.
     *
     * This ensures instead of e.g., /marketplace/items/265/1713, it returns /marketplace/items/265/1713/dist
     */
    fun getInstallationFolder(): File? = installedRevisionDir?.let(::contentFolder)

    suspend fun checkUpdate(): Int? = getNewestRevisionId()

    /**
     * The revision to install, when it is not the installed one: the newest one, or for an add-on
     * the newest one the marketplace offers for this game. That can be older than the installed one.
     */
    suspend fun getNewestRevisionId(): Int? = locked {
        (resolveRevision() as? RevisionResolution.Compatible)?.revision?.id?.takeIf { it != installedRevisionId }
    }

    /**
     * Whether [getNewestRevisionId] has one, without waiting for an install of this item. `false`
     * while an install runs.
     */
    internal suspend fun hasUpdate(): Boolean {
        if (!lock.tryLock()) {
            return false
        }

        return try {
            val revisionId = (resolveRevision() as? RevisionResolution.Compatible)?.revision?.id
            revisionId != null && revisionId != installedRevisionId
        } finally {
            lock.unlock()
        }
    }

    suspend fun install(revisionId: Int, subTask: ResourceTask? = null) {
        locked {
            if (unpack(revisionId, subTask)) {
                reload()
            }
        }
    }

    internal suspend fun <T> locked(block: suspend () -> T): T = lock.withLock {
        restoreRetired()
        block()
    }

    /**
     * An install that stopped between retiring the old revision and placing the new one leaves no
     * numeric directory, and the retired one whole.
     */
    internal fun restoreRetired() {
        if (revisionDirs.isNotEmpty()) {
            return
        }

        val retired = itemDir.listFiles { file: File -> file.isDirectory && retiredName.matches(file.name) }
            ?.singleOrNull() ?: return
        retired.renameTo(itemDir.resolve(retired.name.removePrefix(".").removeSuffix(RETIRED_SUFFIX)))
    }

    /**
     * Makes [revisionId] the only installed revision, `false` when it already is. Callers hold [locked].
     */
    internal suspend fun unpack(revisionId: Int, subTask: ResourceTask? = null): Boolean {
        if (revisionId == installedRevisionId) {
            return false
        }

        commit(fetch(revisionId, subTask), revisionId)
        if (type == MarketplaceItemType.ADDON) {
            AddonInstaller.unpacked(this, revisionId)
        }
        return true
    }

    /**
     * Downloads and unpacks [revisionId] next to the installed revision, where nothing takes it for
     * installed. Callers hold [locked].
     */
    private suspend fun fetch(revisionId: Int, subTask: ResourceTask? = null): File {
        val unpacked = itemDir.resolve(".$revisionId")
        if (unpacked.isDirectory) {
            return unpacked
        }

        check(itemDir.exists() && !itemDir.isFile || itemDir.mkdirs()) {
            itemDir.delete()
            "Failed to create item root directory"
        }

        val part = itemDir.resolve(".$revisionId.part")
        val progress = subTask?.let { subTask ->
            OkHttpProgressInterceptor.ProgressListener { bytesRead, contentLength, _ ->
                subTask.update(bytesRead, contentLength)
            }
        }

        try {
            withContext(Dispatchers.IO) {
                part.deleteRecursively()
                // TODO: Check checksum
                request(MarketplaceApi.downloadRevision(id, revisionId), HttpMethod.GET, progressListener = progress)
                    .use { response -> extractZip(response.body.byteStream(), part) }
            }
            part.rename(unpacked)
        } finally {
            part.deleteRecursively()
        }
        return unpacked
    }

    /**
     * Old revisions are renamed away before they are deleted, since a half-deleted one would still
     * count as installed. They go back when the new one cannot take their place.
     */
    private suspend fun commit(unpacked: File, revisionId: Int) {
        val retired = mutableListOf<Pair<File, File>>()
        try {
            for (dir in revisionDirs) {
                val old = itemDir.resolve(".${dir.name}$RETIRED_SUFFIX")
                old.deleteRecursively()
                dir.rename(old)
                retired += dir to old
            }
            unpacked.rename(itemDir.resolve(revisionId.toString()))
        } catch (e: Exception) {
            retired.forEach { (dir, old) -> old.renameTo(dir) }
            throw e
        }

        itemDir.listFiles { file: File -> file.name.startsWith(".") }?.forEach { it.deleteRecursively() }
    }

    internal suspend fun reload() {
        // Reload the item type's manager on the render thread.
        withContext(MinecraftDispatcher) {
            type.reload()
        }
    }

    /**
     * The single jar an add-on revision in [revisionDir] holds.
     */
    internal fun addonJar(revisionDir: File): File {
        val folder = contentFolder(revisionDir) ?: error("Add-on $id has no files in $revisionDir")
        val jars = folder.listFiles { file: File -> file.isFile && file.extension == "jar" }.orEmpty()
        check(jars.size == 1) {
            "Add-on revision must be an archive containing exactly one jar, found ${jars.size} in $folder"
        }
        return jars.single()
    }

}
