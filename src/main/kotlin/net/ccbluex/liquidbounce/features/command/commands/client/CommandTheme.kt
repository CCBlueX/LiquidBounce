package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.config.AutoConfig.loadAutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.IncludeConfiguration
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import java.io.File

object CommandTheme : Command.Factory  {
    private val themesFolder = File(ConfigSystem.rootFolder, "hud").apply {
        if (!exists()) mkdir()
    }

    private fun getThemeFiles(): List<File> {
        return themesFolder.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    private fun getThemeNames(): List<String> {
        return getThemeFiles().map { it.nameWithoutExtension }
    }

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("theme")
            .hub()
            .subcommand(saveSubcommand())
            .subcommand(loadSubcommand())
            .subcommand(listSubcommand())
            .build()
    }

    private fun saveSubcommand() = CommandBuilder
        .begin("save")
        .alias("create")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<String>("include")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { s, _ ->
                    arrayOf("binds", "hidden").filter { it.startsWith(s) }
                }
                .vararg()
                .optional()
                .build()
        )
        .handler {
            val name = args[0] as String
            val themeFile = themesFolder.resolve("$name.json")
            @Suppress("UNCHECKED_CAST")
            val include = args.getOrNull(1) as Array<*>? ?: emptyArray<String>()

            val includeConfiguration = IncludeConfiguration(
                includeBinds = include.contains("binds"),
                includeHidden = include.contains("hidden")
            )

            runCatching {
                if (themeFile.exists()) themeFile.delete()
                themeFile.createNewFile()
                themeFile.bufferedWriter().use { writer ->
                    serializeAutoConfig(writer, includeConfiguration, modulesToInclude = listOf(ModuleHud))
                }
                chat(regular("§a'$name'保存成功!"))
            }.onFailure {
                chat(regular("§c保存失败: ${it.message}"))
            }
        }
        .build()

    private fun loadSubcommand() = CommandBuilder
        .begin("load")
        .parameter(
            ParameterBuilder
                .begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { begin, _ -> getThemeNames().filter { it.startsWith(begin) } }
                .required()
                .build()
        )
        .handler {
            val name = args[0] as String
            val themeFile = themesFolder.resolve("$name.json")

            runCatching {
                check(themeFile.exists()) { "配置不存在" }
                themeFile.bufferedReader().use { reader ->
                    loadAutoConfig(reader, listOf(ModuleHud), silent = true)
                }
                chat(regular("§a'$name'加载成功!"))
            }.onFailure {
                chat(regular("§c加载失败: ${it.message}"))
            }
        }
        .build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .handler {
            val themes = getThemeNames()
            if (themes.isEmpty()) {
                chat(regular("§7未找到任何HUD配置！"))
                return@handler
            }
            chat(regular("§6可用配置:"))
            themes.forEach { chat(regular("- §f$it")) }
        }
        .build()
}
