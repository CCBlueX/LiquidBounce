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
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Moves marketplace add-ons into the `mods` folder, where Fabric will find them next launch.
 *
 * Add-ons are Fabric mods, so unlike themes they cannot be activated in a running game: the loader
 * resolves entrypoints during startup. Everything here therefore stages files and asks for a
 * restart rather than reloading anything.
 */
object AddonInstaller {

    private val logger = clientLogger("AddonInstaller")

    private const val PREFIX = "liquidbounce-addon-"

    /**
     * Fabric only picks up `.jar` files, so a copy in progress is invisible to it.
     */
    private const val PART_SUFFIX = ".part"

    private val modsFolder: File
        get() = File(mc.gameDirectory, "mods")

    /**
     * A managed filename derived from the item id alone, so an add-on can be located without its
     * marketplace directory. That matters because `MarketplaceManager.unsubscribe` deletes the item
     * directory *before* asking the type to reload.
     */
    private fun managedName(itemId: Int, revisionId: Int) = "$PREFIX$itemId-$revisionId.jar"

    private fun managedFiles(filter: (File) -> Boolean = { true }): List<File> =
        modsFolder.listFiles { file: File -> file.isFile && file.name.startsWith(PREFIX) && filter(file) }
            ?.toList().orEmpty()

    private fun managedJarsFor(itemId: Int): List<File> =
        managedFiles { it.name.startsWith("$PREFIX$itemId-") && it.name.endsWith(".jar") }

    private fun itemIdOf(file: File): Int? = file.name.removePrefix(PREFIX).substringBefore('-').toIntOrNull()

    /**
     * Brings the `mods` folder in line with the current add-on subscriptions: stages any newly
     * installed revision and removes files for add-ons that are gone.
     */
    fun stageSubscribedAddons() {
        val subscribed = MarketplaceManager.getSubscribedItemsOfType(MarketplaceItemType.ADDON)
        val expected = HashSet<String>(subscribed.size)

        for (item in subscribed) {
            val revisionId = item.installedRevisionId ?: continue
            val target = File(modsFolder, managedName(item.id, revisionId))

            runCatching { stage(item, target) }
                .onSuccess { expected += target.name }
                .onFailure { error ->
                    logger.error("Failed to stage add-on '${item.name}' (${item.id})", error)
                    // A broken update must not take the working revision with it.
                    managedJarsFor(item.id).mapTo(expected) { it.name }
                }
        }

        // Anything managed that is no longer subscribed, is a superseded revision, or is a copy
        // that never completed.
        for (file in managedFiles { it.name !in expected }) {
            remove(file)
        }
    }

    private fun stage(item: SubscribedItem, target: File) {
        if (target.exists()) {
            return
        }

        val folder = item.getInstallationFolder()
            ?: error("Add-on ${item.id} has no installation folder")

        val jars = folder.listFiles { file: File -> file.isFile && file.extension == "jar" }.orEmpty()
        check(jars.size == 1) {
            "Add-on revision must be an archive containing exactly one jar, found ${jars.size} in $folder"
        }

        check(modsFolder.isDirectory || modsFolder.mkdirs()) { "Could not create the mods folder" }

        // Written under a name the loader ignores and renamed in one step, so a crash mid-copy
        // never leaves a truncated jar behind for the next launch.
        val part = File(modsFolder, target.name + PART_SUFFIX)
        jars.single().copyTo(part, overwrite = true)

        // Older revisions go only now that the new bytes are safely on disk.
        managedJarsFor(item.id).forEach(::remove)

        try {
            Files.move(part.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        AddonManager.markRestartRequired(item.id, "${item.name} installed")
        logger.info("Staged add-on '${item.name}' as ${target.name}; restart required")
    }

    /**
     * Best-effort deletion.
     *
     * A jar the running JVM has loaded cannot be deleted on Windows, and a `preLaunch` entrypoint
     * would not help, because Fabric runs those *after* mod discovery, so the file is already
     * open by then. [File.deleteOnExit] catches the common case, and the next startup retries
     * whatever is left.
     *
     * Elsewhere the delete succeeds, but the mod's classes stay in memory either way, so a loaded
     * jar asks for a restart no matter how the delete went.
     */
    private fun remove(file: File) {
        val loaded = isLoaded(file)

        if (file.delete()) {
            logger.info("Removed staged add-on ${file.name}")
        } else {
            file.deleteOnExit()
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
     * Whether Fabric loaded a mod from [file] at startup. Fabric records real paths, so a symlinked
     * `mods` folder has to be resolved the same way before comparing.
     */
    private fun isLoaded(file: File): Boolean {
        val path = file.toPath().let { path ->
            runCatching { path.toRealPath() }.getOrDefault(path.toAbsolutePath().normalize())
        }

        return FabricLoader.getInstance().allMods.any { mod ->
            mod.origin.kind == ModOrigin.Kind.PATH && path in mod.origin.paths
        }
    }

    /**
     * Deletes every managed file, including jars staged this session that no mod container knows
     * about yet. Part of the client wipe.
     */
    fun wipeManagedJars() {
        for (file in managedFiles()) {
            if (!file.delete()) {
                file.deleteOnExit()
            }
        }
    }

}
