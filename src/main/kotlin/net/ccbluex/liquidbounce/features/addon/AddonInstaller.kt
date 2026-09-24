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
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.atomicMoveTo
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import java.io.File
import java.io.FileFilter
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.copyTo

/**
 * Fabric discovers mods only at launch, so nothing staged here takes effect before a restart.
 */
@Suppress("TooManyFunctions")
object AddonInstaller {

    private val logger = clientLogger("AddonInstaller")

    private const val PREFIX = "liquidbounce-addon-"

    private const val PART_SUFFIX = ".part"

    private val managedName = Regex("""${Regex.escape(PREFIX)}(\d+)-(\d+)\.jar""")

    private val modsFolder: File
        get() = System.getProperty("fabric.modsFolder")?.let(::File) ?: File(mc.gameDirectory, "mods")

    /**
     * LiquidLauncher rebuilds the mods folder at every start and stages the add-ons itself, so it is
     * left alone here. Minecraft's launch profile passes the launcher name as this property.
     */
    val launcherManaged = System.getProperty("minecraft.launcher.brand") == "LiquidLauncher"

    internal val minecraft: String
        get() = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().metadata.version.friendlyString

    internal val liquidbounce: String
        get() = FabricLoader.getInstance().getModContainer("liquidbounce").orElseThrow().metadata.version.friendlyString

    // Item id to the revision unpacked this session. Fabric refuses to start with an add-on that does not
    // fit, and the user has to remove it, so a jar it did not load is never put back.
    private val unpacked = ConcurrentHashMap<Int, Int>()

    // Windows locks loaded jars. Unlike File.deleteOnExit, a jar that is wanted again can leave this.
    private val removeOnExit = ConcurrentHashMap.newKeySet<File>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread { removeOnExit.forEach(File::delete) })
    }

    // Named by item id, since unsubscribe deletes the item directory before the reload that
    // unstages the jar. LiquidLauncher names the jars it stages the same way.
    private fun managedName(itemId: Int, revisionId: Int) = "$PREFIX$itemId-$revisionId.jar"

    private fun managedFiles(filter: FileFilter = { true }): List<File> =
        modsFolder.listFiles { file: File ->
            file.isFile && file.name.startsWith(PREFIX) && filter.accept(file)
        }?.asList().orEmpty()

    private fun managedJarsFor(itemId: Int): List<File> =
        managedFiles { it.name.startsWith("$PREFIX$itemId-") && it.name.endsWith(".jar") }

    private fun itemIdOf(file: File): Int? = file.name.removePrefix(PREFIX).substringBefore('-').toIntOrNull()

    private class Loaded(val revisionId: Int, val name: String)

    /**
     * Item id to each add-on this game loaded.
     */
    private val loaded: Map<Int, Loaded> by lazy {
        FabricLoader.getInstance().allMods
            .filter { it.origin.kind == ModOrigin.Kind.PATH }
            .flatMap { mod ->
                mod.origin.paths.mapNotNull { path ->
                    managedName.matchEntire(path.fileName?.toString().orEmpty())?.let { match ->
                        match.groupValues[1].toInt() to Loaded(match.groupValues[2].toInt(), mod.metadata.name)
                    }
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
        val expected = HashSet<String>()
        for (item in subscribed) {
            val revisionDir = item.installedRevisionDir ?: continue
            val revisionId = revisionDir.name.toInt()
            val target = File(modsFolder, managedName(item.id, revisionId))
            if (unpacked[item.id] != revisionId && !isLoaded(target)) {
                continue
            }

            runCatching { stage(item, item.addonJar(revisionDir), target) }
                .onSuccess { expected += target.name }
                .onFailure { error ->
                    logger.error("Failed to stage add-on '${item.name}' (${item.id})", error)
                    // Keep the working revision when an update fails.
                    managedJarsFor(item.id).mapTo(expected) { it.name }
                }
        }
        removeOnExit.removeIf { it.name in expected }

        // Unsubscribed, superseded, or a leftover .part.
        for (file in managedFiles { it.name !in expected }) {
            remove(file)
        }
    }

    private fun stage(item: SubscribedItem, jar: File, target: File) {
        if (target.exists()) {
            return
        }

        check(modsFolder.isDirectory || modsFolder.mkdirs()) { "Could not create the mods folder" }

        // Fabric ignores non-jars, so a crash mid-copy leaves no truncated jar behind.
        val part = File(modsFolder, target.name + PART_SUFFIX).toPath()
        jar.toPath().copyTo(part, overwrite = true)

        // Old revisions go only once the copy succeeded.
        managedJarsFor(item.id).forEach(::remove)

        part.atomicMoveTo(target.toPath())
        AddonManager.markRestartRequired(item.id, "${item.name} installed")
        logger.info("Staged add-on '${item.name}' as ${target.name}; restart required")
    }

    /**
     * A loaded jar needs a restart either way.
     */
    private fun remove(file: File) {
        val loaded = isLoaded(file)

        if (deleteNowOrOnExit(file)) {
            logger.info("Removed staged add-on ${file.name}")
        } else {
            logger.warn("Could not delete ${file.name} while it is loaded; scheduled for removal on exit")
        }

        if (file.name.endsWith(PART_SUFFIX)) {
            return
        }

        val itemId = itemIdOf(file) ?: return
        if (loaded) {
            AddonManager.markRestartRequired(itemId, "${file.name} removed")
        } else {
            AddonManager.clearRestartRequired(itemId)
        }
    }

    /**
     * Windows locks loaded jars, so a failed delete is retried on exit and at the next startup.
     */
    private fun deleteNowOrOnExit(file: File): Boolean {
        if (file.delete()) {
            return true
        }

        removeOnExit += file
        return false
    }

    private fun isLoaded(file: File): Boolean {
        // Fabric records real paths.
        val path = file.toPath().let { path ->
            runCatching { path.toRealPath() }.getOrDefault(path.toAbsolutePath().normalize())
        }

        return FabricLoader.getInstance().allMods.any { mod ->
            mod.origin.kind == ModOrigin.Kind.PATH && path in mod.origin.paths
        }
    }

    fun wipeManagedJars() {
        managedFiles().forEach(::deleteNowOrOnExit)
    }

}
