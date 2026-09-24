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
package net.ccbluex.liquidbounce.features.addon

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.SubscribedItem
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.io.atomicMoveTo
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Fabric discovers mods only at launch, so nothing staged here takes effect before a restart.
 */
object AddonInstaller {

    private val logger = clientLogger("AddonInstaller")

    private const val PREFIX = "liquidbounce-addon-"

    private const val PART_SUFFIX = ".part"

    // Where Fabric Loader looks for mods.
    private val modsDir: Path = System.getProperty("fabric.modsFolder")?.let(Path::of)
        ?: FabricLoader.getInstance().gameDir.resolve("mods")

    /**
     * LiquidLauncher rebuilds the mods folder at every start and stages the add-ons itself, so it is
     * left alone here. Minecraft's launch profile passes the launcher name as this property.
     */
    val launcherManaged = System.getProperty("minecraft.launcher.brand") == "LiquidLauncher"

    internal val minecraft by lazy { versionOf("minecraft") }

    internal val liquidbounce by lazy { versionOf("liquidbounce") }

    private fun versionOf(modId: String) =
        FabricLoader.getInstance().getModContainer(modId).orElseThrow().metadata.version.friendlyString

    // Item id to the revision unpacked this session. Fabric refuses to start with an add-on that does not
    // fit, and the user has to remove it, so a jar it did not load is never put back.
    private val unpacked = ConcurrentHashMap<Int, Int>()

    // Windows locks loaded jars. Unlike File.deleteOnExit, a jar that is wanted again can leave this.
    private val removeOnExit = ConcurrentHashMap.newKeySet<Path>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread { removeOnExit.forEach { runCatching { it.deleteIfExists() } } })
    }

    /**
     * The jar of [revisionId] of add-on [itemId] in the mods folder. Named by item id, since unsubscribe
     * deletes the item directory before the reload that unstages the jar. LiquidLauncher names the jars
     * it stages the same way.
     */
    private data class Staged(val itemId: Int, val revisionId: Int) {

        val fileName get() = "$PREFIX$itemId-$revisionId.jar"

        val loaded get() = AddonInstaller.loaded[itemId]?.revisionId == revisionId

        companion {
            private val pattern = Regex("""${Regex.escape(PREFIX)}(\d+)-(\d+)\.jar""")

            fun of(path: Path) = pattern.matchEntire(path.name)?.let { match ->
                Staged(match.groupValues[1].toInt(), match.groupValues[2].toInt())
            }
        }
    }

    private class Loaded(val revisionId: Int, val name: String)

    /**
     * Item id to each add-on this game loaded.
     */
    private val loaded: Map<Int, Loaded> by lazy {
        FabricLoader.getInstance().allMods
            .filter { it.origin.kind == ModOrigin.Kind.PATH }
            .flatMap { mod ->
                mod.origin.paths.mapNotNull { path ->
                    Staged.of(path)?.let { it.itemId to Loaded(it.revisionId, mod.metadata.name) }
                }
            }
            .toMap()
    }

    /**
     * Stages the revision of every subscribed add-on unpacked this session, keeps the loaded ones, and
     * removes every other managed jar. Under LiquidLauncher only tells which add-ons change at the next
     * start.
     */
    fun stageSubscribedAddons() {
        val subscribed = MarketplaceManager.getSubscribedItemsOfType(MarketplaceItemType.ADDON)
        unpacked.keys.retainAll(subscribed.mapTo(HashSet()) { it.id })

        if (launcherManaged) {
            trackLauncherChanges(subscribed)
        } else {
            stage(subscribed)
        }
    }

    internal fun unpacked(item: SubscribedItem, revisionId: Int) {
        unpacked[item.id] = revisionId
    }

    private fun trackLauncherChanges(subscribed: List<SubscribedItem>) {
        val wanted = subscribed.associateBy { it.id }
        for (itemId in wanted.keys + loaded.keys + AddonManager.restartRequiredItems) {
            val item = wanted[itemId]
            val running = loaded[itemId]
            val next = unpacked[itemId]
            when {
                item == null && running != null -> AddonManager.markRestartRequired(itemId, "${running.name} removed")
                item != null && next != null && next != running?.revisionId ->
                    AddonManager.markRestartRequired(itemId, "${item.name} installed")
                else -> AddonManager.clearRestartRequired(itemId)
            }
        }
    }

    private fun stage(subscribed: List<SubscribedItem>) {
        val expected = HashSet<Path>()
        for (item in subscribed) {
            val revisionDir = item.installedRevisionDir ?: continue
            val staged = Staged(item.id, revisionDir.name.toInt())
            if (unpacked[item.id] != staged.revisionId && !staged.loaded) {
                continue
            }

            val target = modsDir.resolve(staged.fileName)
            runCatching { stage(item, item.addonJar(revisionDir), target) }
                .onSuccess { expected.add(target) }
                .onFailure { error ->
                    logger.error("Failed to stage add-on '${item.name}' (${item.id})", error)
                    // Keep the working revision when an update fails.
                    expected.addAll(managedJarsFor(item.id))
                }
        }
        removeOnExit.removeAll(expected)

        // Unsubscribed, superseded, or a leftover .part.
        managedFiles().filterNot(expected::contains).forEach(::remove)
    }

    private fun stage(item: SubscribedItem, jar: File, target: Path) {
        if (target.exists()) {
            return
        }

        modsDir.createDirectories()

        // Fabric ignores non-jars, so a crash mid-copy leaves no truncated jar behind.
        val part = target.resolveSibling(target.name + PART_SUFFIX)
        jar.toPath().copyTo(part, overwrite = true)

        // Old revisions go only once the copy succeeded.
        managedJarsFor(item.id).forEach(::remove)

        part.atomicMoveTo(target)
        AddonManager.markRestartRequired(item.id, "${item.name} installed")
        logger.info("Staged add-on '${item.name}' as ${target.name}; restart required")
    }

    private fun managedFiles(): List<Path> = if (modsDir.isDirectory()) {
        modsDir.listDirectoryEntries("$PREFIX*").filter { it.isRegularFile() }
    } else {
        emptyList()
    }

    private fun managedJarsFor(itemId: Int) = managedFiles().filter { Staged.of(it)?.itemId == itemId }

    /**
     * A loaded jar needs a restart either way.
     */
    private fun remove(file: Path) {
        if (deleteNowOrOnExit(file)) {
            logger.info("Removed staged add-on ${file.name}")
        } else {
            logger.warn("Could not delete ${file.name} while it is loaded; scheduled for removal on exit")
        }

        val staged = Staged.of(file) ?: return
        if (staged.loaded) {
            AddonManager.markRestartRequired(staged.itemId, "${file.name} removed")
        } else {
            AddonManager.clearRestartRequired(staged.itemId)
        }
    }

    /**
     * Windows locks loaded jars, so a failed delete is retried on exit and at the next startup.
     */
    private fun deleteNowOrOnExit(file: Path): Boolean {
        if (runCatching { file.deleteIfExists() }.isSuccess) {
            return true
        }

        removeOnExit.add(file)
        return false
    }

    fun wipeManagedJars() {
        managedFiles().forEach(::deleteNowOrOnExit)
    }

}
