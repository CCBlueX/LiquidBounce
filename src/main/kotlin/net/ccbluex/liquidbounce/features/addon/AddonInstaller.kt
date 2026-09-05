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
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.mc
import java.io.File

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

    private val modsFolder: File
        get() = File(mc.gameDirectory, "mods")

    /**
     * A managed filename derived from the item id alone, so an add-on can be located without its
     * marketplace directory. That matters because `MarketplaceManager.unsubscribe` deletes the item
     * directory *before* asking the type to reload.
     */
    private fun managedName(itemId: Int, revisionId: Int) = "$PREFIX$itemId-$revisionId.jar"

    private fun managedJarsFor(itemId: Int): List<File> =
        modsFolder.listFiles { file: File ->
            file.isFile && file.name.startsWith("$PREFIX$itemId-") && file.name.endsWith(".jar")
        }?.toList().orEmpty()

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
            expected += target.name

            runCatching { stage(item.id, target) }
                .onFailure { logger.error("Failed to stage add-on '${item.name}' (${item.id})", it) }
        }

        // Anything managed that is no longer subscribed - or is a superseded revision.
        val stale = modsFolder.listFiles { file: File ->
            file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(".jar")
        }?.filterNot { it.name in expected }.orEmpty()

        for (file in stale) {
            remove(file)
        }
    }

    private fun stage(itemId: Int, target: File) {
        if (target.exists()) {
            return
        }

        val item = MarketplaceManager.getItem(itemId) ?: return
        val folder = item.getInstallationFolder()
            ?: error("Add-on $itemId has no installation folder")

        val jars = folder.listFiles { file: File -> file.isFile && file.extension == "jar" }.orEmpty()
        check(jars.size == 1) {
            "Add-on revision must be an archive containing exactly one jar, found ${jars.size} in $folder"
        }

        check(modsFolder.isDirectory || modsFolder.mkdirs()) { "Could not create the mods folder" }

        // Drop older revisions of the same add-on first, so the loader never sees two copies.
        managedJarsFor(itemId).forEach(::remove)

        jars.single().copyTo(target, overwrite = true)
        AddonManager.markRestartRequired("${item.name} installed")
        logger.info("Staged add-on '${item.name}' as ${target.name}; restart required")
    }

    /**
     * Best-effort deletion.
     *
     * A jar the running JVM has loaded cannot be deleted on Windows, and a `preLaunch` entrypoint
     * would not help, because Fabric runs those *after* mod discovery, so the file is
 * already open by then.
     * [File.deleteOnExit] catches the common case, and the next startup retries whatever is left.
     */
    private fun remove(file: File) {
        if (file.delete()) {
            logger.info("Removed staged add-on ${file.name}")
            return
        }

        file.deleteOnExit()
        AddonManager.markRestartRequired("${file.name} pending removal")
        logger.warn("Could not delete ${file.name} while it is loaded; scheduled for removal on exit")
    }

}
