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
package net.ccbluex.liquidbounce.features.marketplace.autoconfig

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.API_BRANCH
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemVisibility
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.gson.util.parseTree
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.ValueChangedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.io.File
import java.security.MessageDigest

/**
 * Tracks the marketplace config the client runs.
 *
 * A loaded config is [State.TRACKED] while the settings match what was loaded and
 * [State.EDITING] once they differ. The settings from before the first load are kept
 * as a backup until the user restores or detaches.
 */
@Suppress("TooManyFunctions")
object ConfigTracker : Config("MarketplaceConfig"), EventListener {

    enum class State(override val tag: String) : Tagged {
        NONE("None"),
        TRACKED("Tracked"),
        EDITING("Editing")
    }

    var state by enumChoice("State", State.NONE)
        private set
    var itemId by int("ItemId", 0, 0..Int.MAX_VALUE)
        private set
    var itemName by text("ItemName", "")
        private set
    var itemUid by text("ItemUid", "")
        private set
    var revisionId by int("RevisionId", 0, 0..Int.MAX_VALUE)
        private set
    private var backupName by text("BackupName", "")
    private var baselineHash by text("BaselineHash", "")

    val hasBackup get() = backupName.isNotEmpty()

    private var detectionJob: Job? = null

    @Volatile
    private var suppressDetection = false

    private const val DETECTION_DELAY_MS = 500L

    private val backedUpConfigs get() = listOf(ModuleManager.modulesConfig, SpooferManager)

    private fun cacheFile(itemId: Int, revisionId: Int) =
        MarketplaceManager.marketplaceRoot.resolve("configs/$itemId/$revisionId.json")

    private fun backupFile(name: String) = File(ConfigSystem.backupFolder, "$name.zip")

    @Suppress("unused")
    private val valueChangeHandler = handler<ValueChangedEvent> {
        if (state == State.NONE || suppressDetection) {
            return@handler
        }

        detectionJob?.cancel()
        detectionJob = eventListenerScope.launch {
            delay(DETECTION_DELAY_MS)
            recheck()
        }
    }

    /**
     * Applies [revisionId] of [item]. A full load starts tracking it, a load restricted to
     * [modules] only applies that slice.
     */
    suspend fun load(item: MarketplaceItem, revisionId: Int, modules: Collection<ValueGroup> = emptyList()) {
        val config = readConfig(revisionFile(item.id, revisionId))

        withContext(MinecraftDispatcher) {
            apply(config, modules)

            if (modules.isNotEmpty()) {
                recheck()
                return@withContext
            }

            updateTracking {
                itemId = item.id
                itemName = item.name
                itemUid = item.uid
                this.revisionId = revisionId
                baselineHash = settingsHash()
                state = State.TRACKED
            }
        }
    }

    /**
     * Applies a config that does not come from the marketplace. It stops tracking but keeps
     * the backup.
     */
    suspend fun loadExternal(source: String, modules: Collection<ValueGroup> = emptyList()) {
        val config = publicGson.newJsonReader(source.reader()).use { it.parseTree().asJsonObject }

        withContext(MinecraftDispatcher) {
            apply(config, modules)

            if (modules.isNotEmpty()) {
                recheck()
            } else {
                updateTracking { clearItem() }
            }
        }
    }

    /**
     * Re-applies the tracked revision, dropping local edits.
     */
    suspend fun revert() {
        check(state != State.NONE) { "No tracked config" }

        val config = readConfig(revisionFile(itemId, revisionId))
        withContext(MinecraftDispatcher) {
            apply(config, emptyList())
            updateTracking {
                baselineHash = settingsHash()
                state = State.TRACKED
            }
        }
    }

    /**
     * Returns to the settings from before the first marketplace load.
     */
    suspend fun restoreBackup() = withContext(MinecraftDispatcher) {
        check(hasBackup) { "No backup to restore" }

        val name = backupName
        withoutDetection {
            AutoConfig.withLoading {
                ConfigSystem.restore(name)
            }
        }
        backupFile(name).delete()
        reset()
    }

    /**
     * Keeps the current settings and stops tracking.
     */
    suspend fun detach() = withContext(MinecraftDispatcher) {
        if (hasBackup) {
            backupFile(backupName).delete()
        }
        reset()
    }

    /**
     * Publishes the current settings as a new config and tracks it.
     */
    suspend fun create(
        session: OAuthSession,
        name: String,
        description: String,
        details: MarketplaceApi.ItemDetails,
    ): MarketplaceItem {
        val item = MarketplaceApi.createMarketplaceItem(
            session,
            name,
            MarketplaceItemType.CONFIG,
            description,
            details.copy(branch = API_BRANCH)
        )

        val revision = try {
            uploadSettings(session, item.id, null)
        } catch (e: Exception) {
            runCatching { MarketplaceApi.deleteMarketplaceItem(session, item.id) }
            throw e
        }

        withContext(MinecraftDispatcher) {
            updateTracking {
                itemId = item.id
                itemName = item.name
                itemUid = item.uid
                this.revisionId = revision.id
                baselineHash = settingsHash()
                state = State.TRACKED
            }
        }
        return item
    }

    /**
     * Publishes the edited settings as a new config that links back to the tracked revision.
     */
    suspend fun fork(
        session: OAuthSession,
        name: String,
        description: String,
        visibility: MarketplaceItemVisibility,
    ): MarketplaceItem {
        check(state == State.EDITING) { "Not editing a config" }

        return create(
            session,
            name,
            description,
            MarketplaceApi.ItemDetails(
                visibility = visibility,
                forkedFromItemId = itemId,
                forkedFromRevisionId = revisionId
            )
        )
    }

    /**
     * Uploads the edited settings as the tracked config's newest revision.
     */
    suspend fun update(session: OAuthSession, changelog: String?): MarketplaceItemRevision {
        check(state == State.EDITING) { "Not editing a config" }

        val revision = uploadSettings(session, itemId, changelog)
        withContext(MinecraftDispatcher) {
            updateTracking {
                revisionId = revision.id
                baselineHash = settingsHash()
                state = State.TRACKED
            }
        }
        return revision
    }

    fun renamed(item: MarketplaceItem) {
        if (state != State.NONE && item.id == itemId && item.name != itemName) {
            updateTracking { itemName = item.name }
        }
    }

    suspend fun delete(session: OAuthSession) {
        check(state != State.NONE) { "No tracked config" }

        val id = itemId
        MarketplaceApi.deleteMarketplaceItem(session, id)
        MarketplaceManager.marketplaceRoot.resolve("configs/$id").deleteRecursively()
        detach()
    }

    private suspend fun uploadSettings(
        session: OAuthSession,
        itemId: Int,
        changelog: String?
    ): MarketplaceItemRevision {
        val file = File.createTempFile("marketplace_config", ".json")
        try {
            withContext(MinecraftDispatcher) {
                file.bufferedWriter().use { AutoConfig.serializeAutoConfig(it) }
            }

            val revision = MarketplaceApi.createMarketplaceItemRevision(
                session,
                itemId,
                file,
                version = LiquidBounce.clientVersion,
                changelog = changelog,
                includesBinds = false
            )

            file.copyTo(cacheFile(itemId, revision.id), overwrite = true)
            return revision
        } finally {
            file.delete()
        }
    }

    private suspend fun revisionFile(itemId: Int, revisionId: Int): File {
        val file = cacheFile(itemId, revisionId)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            val part = File(file.parentFile, "${file.name}.part")
            HttpClient.download(MarketplaceApi.downloadRevision(itemId, revisionId), part)
            check(part.renameTo(file)) { "Failed to store revision $revisionId" }
        }
        return file
    }

    private fun readConfig(file: File): JsonObject =
        publicGson.newJsonReader(file.bufferedReader()).use { it.parseTree().asJsonObject }

    /**
     * Backs up the current settings unless a backup exists, then applies [config]. A failed
     * apply puts back the backup it just took.
     */
    private fun apply(config: JsonObject, modules: Collection<ValueGroup>) {
        val createdBackup = !hasBackup || !backupFile(backupName).exists()
        if (createdBackup) {
            val name = "marketplace_preload_${System.currentTimeMillis()}"
            ConfigSystem.backup(name, backedUpConfigs)
            updateTracking { backupName = name }
        }

        withoutDetection {
            try {
                AutoConfig.withLoading {
                    AutoConfig.loadAutoConfig(config, modules)
                }
            } catch (e: Exception) {
                if (createdBackup) {
                    AutoConfig.withLoading {
                        ConfigSystem.restore(backupName)
                    }
                    backupFile(backupName).delete()
                    updateTracking { backupName = "" }
                }
                throw e
            }
        }
    }

    private fun recheck() {
        if (state == State.NONE) {
            return
        }

        val next = if (settingsHash() == baselineHash) State.TRACKED else State.EDITING
        if (next != state) {
            updateTracking { state = next }
        }
    }

    /**
     * Only modules and spoofers count; export metadata such as date and server would make
     * every snapshot differ.
     */
    private fun settingsHash(): String {
        val snapshot = JsonArray().apply {
            add(ConfigSystem.serializeValueGroup(ModuleManager.modulesConfig, publicGson))
            add(ConfigSystem.serializeValueGroup(SpooferManager, publicGson))
        }

        return MessageDigest.getInstance("SHA-256")
            .digest(snapshot.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun clearItem() {
        state = State.NONE
        itemId = 0
        itemName = ""
        itemUid = ""
        revisionId = 0
        baselineHash = ""
    }

    private fun reset() = updateTracking {
        clearItem()
        backupName = ""
    }

    private inline fun withoutDetection(block: () -> Unit) {
        suppressDetection = true
        try {
            block()
        } finally {
            suppressDetection = false
        }
    }

    private inline fun updateTracking(block: ConfigTracker.() -> Unit) {
        withoutDetection { block() }
        detectionJob?.cancel()
        ConfigSystem.store(this)
    }

}
