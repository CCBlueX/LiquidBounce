package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import org.apache.commons.io.IOUtils
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipFile

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class ScriptManagerCommand : Command("scriptmanager", arrayOf("scripts")) {

    override fun execute(args: Array<String>) {
        if(args.size > 1) {
            when {
                args[1].equals("import", true) -> {
                    try {
                        val file = MiscUtils.openFileChooser() ?: return
                        val fileName = file.name

                        if (fileName.endsWith(".js")) {
                            LiquidBounce.CLIENT.scriptManager.importScript(file)

                            LiquidBounce.CLIENT.clickGui = ClickGui()
                            LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.clickGuiConfig)

                            chat("Successfully imported script.")
                            return
                        } else if (fileName.endsWith(".zip")) {
                            val zipFile = ZipFile(file)
                            val entries = zipFile.entries()
                            val scriptFiles = ArrayList<File>()

                            while (entries.hasMoreElements()) {
                                val entry = entries.nextElement()
                                val entryName = entry.name
                                val entryFile = File(LiquidBounce.CLIENT.scriptManager.scriptsFolder, entryName)

                                if (entry.isDirectory) {
                                    entryFile.mkdir()
                                    continue
                                }

                                val fileStream = zipFile.getInputStream(entry)
                                val fileOutputStream = FileOutputStream(entryFile)

                                IOUtils.copy(fileStream, fileOutputStream)
                                fileOutputStream.close()
                                fileStream.close()

                                if (!entryName.contains("/"))
                                    scriptFiles.add(entryFile)
                            }

                            scriptFiles.forEach { scriptFile -> LiquidBounce.CLIENT.scriptManager.loadScript(scriptFile) }

                            LiquidBounce.CLIENT.clickGui = ClickGui()
                            LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.clickGuiConfig)
                            LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.hudConfig)

                            chat("Successfully imported script.")
                            return
                        }

                        chat("The file extension has to be .js or .zip")
                    } catch(t : Throwable) {
                        ClientUtils.getLogger().error("Something went wrong while importing a script.", t)
                        chat("${t.javaClass.name}: ${t.message}")
                    }
                }

                args[1].equals("delete", true) -> {
                    try {
                        if(args.size <= 2) {
                            chatSyntax("scriptmanager delete <index>")
                            return
                        }

                        val scriptIndex = args[2].toInt()
                        val scripts = LiquidBounce.CLIENT.scriptManager.scripts

                        if(scriptIndex >= scripts.size) {
                            chat("Index $scriptIndex is too high.")
                            return
                        }

                        val script = scripts[scriptIndex]

                        LiquidBounce.CLIENT.scriptManager.deleteScript(script)

                        LiquidBounce.CLIENT.clickGui = ClickGui()
                        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.clickGuiConfig)
                        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.hudConfig)
                        chat("Successfully deleted script.")
                    } catch(numberFormat : NumberFormatException) {
                        chatSyntaxError()
                    } catch(t : Throwable) {
                        ClientUtils.getLogger().error("Something went wrong while deleting a script.", t)
                        chat("${t.javaClass.name}: ${t.message}")
                    }
                }

                args[1].equals("reload", true) -> {
                    try {
                        LiquidBounce.CLIENT.scriptManager.reloadScripts()
                        chat("Successfully reloaded all scripts.")
                    } catch(t : Throwable) {
                        ClientUtils.getLogger().error("Something went wrong while reloading all scripts.", t)
                        chat("${t.javaClass.name}: ${t.message}")
                    }
                }

                args[1].equals("folder", true) -> {
                    try {
                        Desktop.getDesktop().open(LiquidBounce.CLIENT.scriptManager.scriptsFolder)
                        chat("Successfully opened scripts folder.")
                    } catch(t : Throwable) {
                        ClientUtils.getLogger().error("Something went wrong while trying to open your scripts folder.", t)
                        chat("${t.javaClass.name}: ${t.message}")
                    }
                }
            }

            return
        }

        val scriptManager = LiquidBounce.CLIENT.scriptManager

        if(scriptManager.scripts.isNotEmpty()) {
            chat("§c§lScripts")
            scriptManager.scripts.forEachIndexed { index, script -> chat("$index: §a§l${script.scriptName} §a§lv${script.scriptVersion} §3by §a§l${script.scriptAuthor}") }
        }

        chatSyntax("scriptmanager <import/delete/reload/folder>")
    }
}