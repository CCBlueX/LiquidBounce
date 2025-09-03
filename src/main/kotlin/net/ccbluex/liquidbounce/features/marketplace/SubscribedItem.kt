package net.ccbluex.liquidbounce.features.marketplace

import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.api.core.HttpClient.download
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.integration.task.type.ResourceTask
import net.ccbluex.liquidbounce.mcef.listeners.OkHttpProgressInterceptor
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.io.File

data class SubscribedItem(val name: String, val id: Int, val type: MarketplaceItemType, var installedRevisionId: Int?) {

    constructor(item: MarketplaceItem) : this(item.name, item.id, item.type, null) {
        require(item.type.isSubscribable) { "Type ${item.type} is not subscribable" }
    }

    val itemDir
        get() = MarketplaceManager.marketplaceRoot.resolve("items/$id")

    /**
     * Get the installation folder of the item.
     *
     * Walks down the revision folder until it finds a file,
     * which returns the parent folder of that file,
     * as the installation folder.
     *
     * This ensures instead of e.g., /marketplace/items/265/1713, it returns /marketplace/items/265/1713/dist
     */
    fun getInstallationFolder(): File? {
        val installedRevisionId = installedRevisionId ?: return null
        val folder = itemDir.resolve(installedRevisionId.toString())
        if (!folder.exists() || !folder.isDirectory) {
            return null
        }

        fun File.containsFile(): Boolean {
            return this.isDirectory && !this.listFiles(File::isFile).isNullOrEmpty()
        }

        if (folder.containsFile()) {
            return folder
        }

        // Return null if no files found at folder or one level below
        return folder.listFiles(File::isDirectory)?.firstOrNull { subFolder ->
            subFolder.containsFile()
        }
    }

    suspend fun checkUpdate(): Int? {
        val newestRevisionId = getNewestRevisionId()
        if (installedRevisionId == newestRevisionId) {
            return null
        }
        return newestRevisionId
    }

    /**
     * Check if the item has an update available.
     *
     * This depends on what item revision is being returned
     * by the Marketplace API as first item. We do not
     * use versioning here, therefore it could also work as downgrade.
     */
    suspend fun getNewestRevisionId(): Int? {
        val item = MarketplaceApi.getMarketplaceItem(id)

        // If the [item] is not active, we don't want to update it.
        if (item.status != MarketplaceItemStatus.ACTIVE) {
            return null
        }

        // Get the newest revision of the item.
        val revisions = MarketplaceApi.getMarketplaceItemRevisions(id, 1, 1)
        if (revisions.items.isEmpty()) {
            return null
        }

        val newestRevisionId = revisions.items[0].id

        val installedRevisionId = installedRevisionId ?: return newestRevisionId
        return if (installedRevisionId != newestRevisionId) {
            newestRevisionId
        } else {
            null
        }
    }

    suspend fun install(revisionId: Int, subTask: ResourceTask? = null) {
        // The revision is already installed, no need to install it again.
        if (revisionId == installedRevisionId) {
            return
        }

        check(itemDir.exists() && !itemDir.isFile || itemDir.mkdirs()) {
            itemDir.delete()
            "Failed to create item root directory"
        }

        val revisionUrl = MarketplaceApi.downloadRevision(id, revisionId)
        val revisionArchiveFile = itemDir.resolve("$id.zip")
        check(!revisionArchiveFile.exists() || revisionArchiveFile.delete()) {
            "Failed to delete existing revision file"
        }

        val revisionDir = itemDir.resolve(revisionId.toString())
        val previousRevisionDir = installedRevisionId?.let { itemDir.resolve(it.toString()) }

        try {
            val taskProgressUpdater = subTask?.let { subTask ->
                OkHttpProgressInterceptor.ProgressListener { bytesRead, contentLength, _ ->
                    subTask.update(bytesRead, contentLength)
                }
            }

            download(revisionUrl, revisionArchiveFile, progressListener = taskProgressUpdater)
            // TODO: Check checksum
            extractZip(revisionArchiveFile, revisionDir)

            installedRevisionId = revisionId
            ConfigSystem.store(MarketplaceManager)
        } catch (exception: Exception) {
            if (revisionDir.exists()) {
                revisionDir.deleteRecursively()
            }

            throw exception
        } finally {
            revisionArchiveFile.delete()
        }

        try {
            previousRevisionDir?.deleteRecursively()
        } catch (exception: Exception) {
            logger.warn("Failed to delete previous revision directory", exception)
        }

        // Reload the item type's manager on the render thread.
        withContext(MinecraftDispatcher) {
            type.reload()
        }
    }


}
