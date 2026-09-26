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

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.event.events.ValueChangedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.NoCompatibleRevisionException
import net.ccbluex.liquidbounce.features.marketplace.Unavailable
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import java.io.File
import java.security.MessageDigest

/**
 * Tracks the marketplace config the client runs.
 *
 * A loaded config is [State.TRACKED] while the settings match what was loaded and
 * [State.EDITING] once they differ. The settings from before the first load are kept
 * as a backup until the user restores or detaches.
 *
 * A config's config dependencies are applied before it, depth-first in their order, and
 * its add-on and script dependencies are installed. The settings after the dependencies are
 * its base: an overlay only publishes what differs from it.
 *
 * Loading a local config over it stops tracking, unless the local config was saved from it; see [loadedLocal].
 */
@Suppress("TooManyFunctions")
object ConfigTracker : Config("MarketplaceConfig"), EventListener {

    enum class State(override val tag: String) : Tagged {
        NONE("None"),
        TRACKED("Tracked"),
        EDITING("Editing")
    }

    data class Step(val itemId: Int, val revisionId: Int)

    /**
     * Dependencies [load] installed; [restartRequired] when one only works after a restart. [unavailable]
     * are the ones left out since they or what they need do not load with this game.
     */
    data class LoadResult(
        val installed: List<MarketplaceItem>,
        val restartRequired: Boolean,
        val unavailable: List<Unavailable>
    )

    var state by enumChoice("State", State.NONE)
        private set
    var itemId by int("ItemId", 0, 0..Int.MAX_VALUE)
        private set
    var itemName by text("ItemName", "")
        private set
    var itemUid by text("ItemUid", "")
        private set
    var itemAuthor by text("ItemAuthor", "")
        private set
    var revisionId by int("RevisionId", 0, 0..Int.MAX_VALUE)
        private set

    /**
     * The local config that last replaced the settings, until a marketplace config does.
     */
    var localName by text("LocalName", "")
        private set

    private var backupName by text("BackupName", "")
    private var chainText by text("Chain", "")
    private var baseText by text("Base", "")
    private var baselineText by text("Baseline", "")

    val hasBackup get() = backupName.isNotEmpty()

    /**
     * Config dependencies applied before the tracked config, in order.
     */
    val chain: List<Step>
        get() = chainText.split(',').filter(String::isNotEmpty).map { step ->
            val (itemId, revisionId) = step.split(':').map(String::toInt)
            Step(itemId, revisionId)
        }

    val hasBase get() = baseText.isNotEmpty()

    /**
     * `author/name` of the tracked config, or its name while the author is unknown.
     */
    val address get() = if (itemAuthor.isEmpty()) itemName else "$itemAuthor/$itemName"

    private var detectionJob: Job? = null

    @Volatile
    private var suppressDetection = false

    private const val DETECTION_DELAY_MS = 500L
    private const val SPOOFERS = "#spoofers"

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
     * Applies [revisionId] of [item] after its config dependencies and installs its add-on and
     * script dependencies. A full load starts tracking it, a load restricted to [modules] only
     * applies that slice of every step.
     */
    suspend fun load(
        item: MarketplaceItem,
        revisionId: Int,
        modules: Collection<ValueGroup> = emptyList()
    ): LoadResult {
        val dependencies = resolve(item.id)
        val (installed, unavailable) = install(dependencies.installables)
        val chain = dependencies.configs
        val configs = (chain + Step(item.id, revisionId)).map { readConfig(revisionFile(it.itemId, it.revisionId)) }

        withContext(Dispatchers.Main) {
            val base = apply(configs, modules)

            if (modules.isNotEmpty()) {
                recheck()
                return@withContext
            }

            updateTracking {
                itemId = item.id
                itemName = item.name
                itemUid = item.uid
                itemAuthor = item.author.orEmpty()
                this.revisionId = revisionId
                chainText = encodeChain(chain)
                baseText = base?.let(::encodeHashes).orEmpty()
                baselineText = encodeHashes(snapshot())
                state = State.TRACKED
                localName = ""
            }
        }

        return LoadResult(
            installed,
            installed.any {
                it.type == MarketplaceItemType.ADDON ||
                    it.type == MarketplaceItemType.SCRIPT && !MarketplaceManager.hasHandler(it.type)
            },
            unavailable
        )
    }

    /**
     * Applies a config that does not come from the marketplace. It stops tracking but keeps
     * the backup.
     */
    suspend fun loadExternal(source: String, modules: Collection<ValueGroup> = emptyList()) {
        val config = publicGson.newJsonReader(source.reader()).use { it.parseTree().asJsonObject }

        withContext(Dispatchers.Main) {
            apply(listOf(config), modules)

            if (modules.isNotEmpty()) {
                recheck()
            } else {
                updateTracking { clearItem() }
            }
        }
    }

    /**
     * Id of the tracked config, which a local config saved now keeps as its origin.
     */
    val trackedItemId get() = itemId.takeIf { state != State.NONE }

    /**
     * Settings of the local config [name] were applied, only some modules of it when [partial]. A partial load,
     * or one of a config saved from the tracked one ([origin] is its [trackedItemId]), goes on as edits of the
     * tracked config. Any other replaces it: tracking stops, the backup stays.
     *
     * @return the address of the config no longer tracked, if any
     */
    fun loadedLocal(name: String, origin: Int?, partial: Boolean): String? {
        if (partial || state != State.NONE && origin == itemId) {
            recheck()
            return null
        }

        val untracked = address.takeIf { state != State.NONE }
        updateTracking {
            clearItem()
            localName = name
        }
        return untracked
    }

    /**
     * Re-applies the tracked revision and its config dependencies, dropping local edits.
     */
    suspend fun revert() {
        check(state != State.NONE) { "No tracked config" }

        val configs = (chain + Step(itemId, revisionId)).map { readConfig(revisionFile(it.itemId, it.revisionId)) }
        withContext(Dispatchers.Main) {
            apply(configs, emptyList())
            updateTracking {
                baselineText = encodeHashes(snapshot())
                state = State.TRACKED
            }
        }
    }

    /**
     * Returns to the settings from before the first marketplace load.
     */
    suspend fun restoreBackup() = withContext(Dispatchers.Main) {
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
    suspend fun detach() = withContext(Dispatchers.Main) {
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
        val (item, revision) = publish(session, name, description, details, null) { }

        withContext(Dispatchers.Main) {
            track(item, revision, emptyList(), null)
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
     * Publishes only what was changed on top of the tracked config, as a new config that depends
     * on it, and tracks that.
     */
    suspend fun overlay(
        session: OAuthSession,
        name: String,
        description: String,
        visibility: MarketplaceItemVisibility,
    ): MarketplaceItem {
        check(state == State.EDITING) { "Not editing a config" }

        val base = decodeHashes(baselineText)
        val chain = chain + Step(itemId, revisionId)
        val baseId = itemId
        val (item, revision) = publish(
            session,
            name,
            description,
            MarketplaceApi.ItemDetails(visibility = visibility),
            withContext(Dispatchers.Main) { changedSince(base) }
        ) { item -> MarketplaceApi.addItemDependency(session, item.id, baseId) }

        withContext(Dispatchers.Main) {
            track(item, revision, chain, base)
        }
        return item
    }

    /**
     * Uploads the edited settings as the tracked config's newest revision. With config
     * dependencies, only what differs from them.
     */
    suspend fun update(session: OAuthSession, changelog: String?): MarketplaceItemRevision {
        check(state == State.EDITING) { "Not editing a config" }

        val subset = if (hasBase) {
            withContext(Dispatchers.Main) { changedSince(decodeHashes(baseText)) }
        } else {
            null
        }
        val revision = uploadSettings(session, itemId, changelog, subset)
        withContext(Dispatchers.Main) {
            updateTracking {
                revisionId = revision.id
                baselineText = encodeHashes(snapshot())
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

    private class Dependencies(val configs: List<Step>, val installables: Collection<Installable>)

    /**
     * An add-on or script, and the add-ons and scripts it [needs].
     */
    private class Installable(val item: MarketplaceItem, val needs: Set<Int>)

    /**
     * Walks the dependencies of [rootId] depth-first. Config dependencies come out in the order
     * they are applied, installables after the ones they need, so an add-on is checked with those
     * installed.
     */
    private suspend fun resolve(rootId: Int): Dependencies {
        val configs = mutableListOf<Step>()
        val installables = linkedMapOf<Int, Installable>()
        val visiting = hashSetOf<Int>()
        val needsOf = hashMapOf<Int, Set<Int>>()

        suspend fun visit(id: Int): Set<Int> {
            needsOf[id]?.let { return it }
            if (!visiting.add(id)) {
                return emptySet()
            }

            val needs = linkedSetOf<Int>()
            for (dependency in MarketplaceApi.getItemDependencies(id)) {
                val item = dependency.item
                when (item.type) {
                    MarketplaceItemType.CONFIG -> {
                        visit(item.id)
                        val revision = dependency.liveRevision
                            ?: error("Config dependency ${item.name} has nothing published")
                        if (configs.none { it.itemId == item.id }) {
                            configs += Step(item.id, revision.id)
                        }
                    }

                    MarketplaceItemType.ADDON, MarketplaceItemType.SCRIPT -> {
                        installables.putIfAbsent(item.id, Installable(item, visit(item.id)))
                        needs += item.id
                    }

                    else -> {}
                }
            }

            visiting.remove(id)
            needsOf[id] = needs
            return needs
        }

        visit(rootId)
        return Dependencies(configs, installables.values)
    }

    /**
     * Subscribes to the [installables] that are missing. One that does not load with this game does not
     * stop the load; it and what needs it are left out, and the second list names why.
     */
    private suspend fun install(
        installables: Collection<Installable>
    ): Pair<List<MarketplaceItem>, List<Unavailable>> {
        val installed = mutableListOf<MarketplaceItem>()
        val leftOut = linkedMapOf<Int, Unavailable>()
        for (installable in installables) {
            val item = installable.item
            if (MarketplaceManager.isSubscribed(item.id)) {
                continue
            }

            val reason = installable.needs.firstNotNullOfOrNull { leftOut[it] }
                ?: Unavailable(item.name, false).takeIf { item.status != MarketplaceItemStatus.ACTIVE }
            if (reason != null) {
                leftOut[item.id] = reason
                continue
            }

            try {
                MarketplaceManager.subscribe(item)
                installed += item
            } catch (e: NoCompatibleRevisionException) {
                leftOut[item.id] = e.unavailable
            }
        }
        return installed to leftOut.values.distinct()
    }

    /**
     * Creates the item, runs [link] on it and uploads the settings (only [subset] when given). A
     * failed step deletes the item again.
     */
    private suspend inline fun publish(
        session: OAuthSession,
        name: String,
        description: String,
        details: MarketplaceApi.ItemDetails,
        subset: Subset?,
        link: (MarketplaceItem) -> Unit,
    ): Pair<MarketplaceItem, MarketplaceItemRevision> {
        val item = MarketplaceApi.createMarketplaceItem(
            session,
            name,
            MarketplaceItemType.CONFIG,
            description,
            details.copy(branch = API_BRANCH)
        )

        val revision = try {
            link(item)
            uploadSettings(session, item.id, null, subset)
        } catch (e: Exception) {
            runCatching { MarketplaceApi.deleteMarketplaceItem(session, item.id) }
            throw e
        }
        return item to revision
    }

    private fun track(
        item: MarketplaceItem,
        revision: MarketplaceItemRevision,
        chain: List<Step>,
        base: Map<String, String>?
    ) = updateTracking {
        itemId = item.id
        itemName = item.name
        itemUid = item.uid
        itemAuthor = item.author.orEmpty()
        revisionId = revision.id
        chainText = encodeChain(chain)
        baseText = base?.let(::encodeHashes).orEmpty()
        baselineText = encodeHashes(snapshot())
        state = State.TRACKED
        localName = ""
    }

    private class Subset(val modules: Set<String>, val spoofers: Boolean)

    private fun changedSince(hashes: Map<String, String>): Subset {
        val changed = snapshot().filter { (name, hash) -> hashes[name] != hash }.keys
        return Subset(changed - SPOOFERS, SPOOFERS in changed)
    }

    private suspend fun uploadSettings(
        session: OAuthSession,
        itemId: Int,
        changelog: String?,
        subset: Subset?
    ): MarketplaceItemRevision {
        val file = File.createTempFile("marketplace_config", ".json")
        try {
            withContext(Dispatchers.Main) {
                file.bufferedWriter().use { writer ->
                    if (subset == null) {
                        AutoConfig.serializeAutoConfig(writer)
                    } else {
                        AutoConfig.serializeAutoConfig(
                            writer,
                            modules = subset.modules,
                            includeSpoofers = subset.spoofers
                        )
                    }
                }
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
     * Backs up the current settings unless a backup exists, then applies [configs] in order. A
     * failed apply puts back the backup it just took.
     *
     * @return the settings before the last config, when there was more than one
     */
    private fun apply(configs: List<JsonObject>, modules: Collection<ValueGroup>): Map<String, String>? {
        val createdBackup = !hasBackup || !backupFile(backupName).exists()
        if (createdBackup) {
            val name = "marketplace_preload_${System.currentTimeMillis()}"
            ConfigSystem.backup(name, backedUpConfigs)
            updateTracking { backupName = name }
        }

        var base: Map<String, String>? = null
        withoutDetection {
            try {
                AutoConfig.withLoading {
                    configs.forEachIndexed { index, config ->
                        if (index == configs.lastIndex && index > 0) {
                            base = snapshot()
                        }
                        AutoConfig.loadAutoConfig(config, modules)
                    }
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
        return base
    }

    private fun recheck() {
        if (state == State.NONE) {
            return
        }

        val next = if (snapshot() == decodeHashes(baselineText)) State.TRACKED else State.EDITING
        if (next != state) {
            updateTracking { state = next }
        }
    }

    /**
     * A hash per module and one for the spoofers. Export metadata such as date and server
     * would make every snapshot differ, so it stays out.
     */
    private fun snapshot(): Map<String, String> {
        val hashes = linkedMapOf<String, String>()
        ConfigSystem.serializeValueGroup(ModuleManager.modulesConfig, publicGson)
            .asJsonObject["value"].asJsonArray.forEach { module ->
                hashes[module.asJsonObject["name"].asString] = sha256(module.toString())
            }
        hashes[SPOOFERS] = sha256(ConfigSystem.serializeValueGroup(SpooferManager, publicGson).toString())
        return hashes
    }

    private fun sha256(text: String) = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun encodeHashes(hashes: Map<String, String>) = publicGson.toJson(hashes)

    private fun decodeHashes(text: String): Map<String, String> =
        if (text.isEmpty()) {
            emptyMap()
        } else {
            publicGson.fromJson(text, JsonObject::class.java).entrySet().associate { it.key to it.value.asString }
        }

    private fun encodeChain(chain: List<Step>) = chain.joinToString(",") { "${it.itemId}:${it.revisionId}" }

    private fun clearItem() {
        state = State.NONE
        localName = ""
        itemId = 0
        itemName = ""
        itemUid = ""
        itemAuthor = ""
        revisionId = 0
        chainText = ""
        baseText = ""
        baselineText = ""
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
        EventManager.callEvent(RefreshArrayListEvent)
    }

}
