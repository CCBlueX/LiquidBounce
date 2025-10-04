/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.task.NodeTask
import groovy.json.JsonOutput
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.gradle.kotlin.dsl.support.listFilesOrdered

plugins {
    id("fabric-loom")
    kotlin("jvm")
    id("com.gorylenko.gradle-git-properties") version "2.5.3"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("com.github.node-gradle.node") version "7.1.0"
    id("org.jetbrains.dokka") version "1.9.10"
}

base {
    archivesName = project.property("archives_base_name") as String
    version = project.property("mod_version") as String
    group = project.property("maven_group") as String
}

/** Includes non-mod dependency recursively in the JAR file */
val includeDependency: Configuration by configurations.creating

/** Includes mod in the JAR file */
val includeModDependency: Configuration by configurations.creating

/** Includes native-only dependency in the JAR file */
val includeNative: Configuration by configurations.creating

/**
 * Provided by:
 * - Minecraft
 * - Mod dependencies
 */
fun Configuration.excludeProvidedLibs() = apply {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")

    exclude(group = "com.google.code.gson", module = "gson")
    exclude(group = "net.java.dev.jna", module = "jna")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "commons-io", module = "commons-io")
    exclude(group = "org.apache.commons", module = "commons-compress")
    exclude(group = "org.apache.commons", module = "commons-lang3")
    exclude(group = "org.apache.logging.log4j", module = "log4j-core")
    exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j-impl")
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "com.mojang", module = "authlib")

    // Note: from Netty HTTP Server, not all components are used
    exclude(group = "io.netty", module = "netty-all")

    exclude(group = "io.netty", module = "netty-buffer")
    exclude(group = "io.netty", module = "netty-codec")
    exclude(group = "io.netty", module = "netty-common")
    exclude(group = "io.netty", module = "netty-handler")
    exclude(group = "io.netty", module = "netty-resolver")
    exclude(group = "io.netty", module = "netty-transport")
    exclude(group = "io.netty", module = "netty-transport-native-unix-common")
}

includeDependency.excludeProvidedLibs()
includeModDependency.excludeProvidedLibs()

configurations {
    include.configure {
        extendsFrom(includeModDependency)
        extendsFrom(includeNative)
    }
    modApi.configure {
        extendsFrom(includeModDependency)
    }
    runtimeOnly.configure {
        extendsFrom(includeNative)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "CCBlueX"
        url = uri("https://maven.ccbluex.net/releases")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/")
    }
    maven {
        name = "ViaVersion"
        url = uri("https://repo.viaversion.com/")
    }
    maven {
        name = "modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        name = "OpenCollab Snapshots"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
    maven {
        name = "Lenni0451"
        url = uri("https://maven.lenni0451.net/everything")
    }
}

loom {
    accessWidenerPath = file("src/main/resources/liquidbounce.accesswidener")
}

dependencies {
    // Minecraft
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")

    // Fabric
    modApi("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modApi("net.fabricmc:fabric-language-kotlin:${project.property("fabric_kotlin_version")}")

    // Mod menu
    modApi("com.terraformersmc:modmenu:${project.property("mod_menu_version")}")

    // Recommended mods (on IDE)
    modApi("maven.modrinth:sodium:${project.property("sodium_version")}")
    modApi("maven.modrinth:lithium:${project.property("lithium_version")}")

    // ViaFabricPlus
    modApi("com.viaversion:viafabricplus-api:${project.property("viafabricplus_version")}")
    modRuntimeOnly("com.viaversion:viafabricplus:${project.property("viafabricplus_version")}")

    // Minecraft Authlib
    includeDependency("com.github.CCBlueX:mc-authlib:${project.property("mc_authlib_version")}")

    // JCEF Support
    includeModDependency("com.github.CCBlueX:mcef:${project.property("mcef_version")}")
    includeDependency("net.ccbluex:netty-httpserver:2.3.2")
    // MacOS native (Linux native is included in game)
    includeDependency("io.netty:netty-transport-classes-kqueue:${project.property("netty_version")}")
    includeNative("io.netty:netty-transport-native-kqueue:${project.property("netty_version")}:osx-aarch_64")
    includeNative("io.netty:netty-transport-native-kqueue:${project.property("netty_version")}:osx-x86_64")

    // Discord RPC Support
    includeDependency("com.github.CCBlueX:DiscordIPC:4.0.0")

    // ScriptAPI
    includeDependency("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")
    includeDependency("org.graalvm.polyglot:polyglot:${project.property("polyglot_version")}")
    includeDependency("org.graalvm.polyglot:js-community:${project.property("polyglot_version")}")
    includeDependency("org.graalvm.polyglot:tools-community:${project.property("polyglot_version")}")
//    includeDependency("org.graalvm.polyglot:python-community:${project.property("polyglot_version")}")
//    includeDependency("org.graalvm.polyglot:wasm-community:${project.property("polyglot_version")}")
//    includeDependency("org.graalvm.polyglot:java-community:${project.property("polyglot_version")}")
//    includeDependency("org.graalvm.polyglot:ruby-community:${project.property("polyglot_version")}")
//    includeDependency("org.graalvm.polyglot:llvm-native-community:${project.property("polyglot_version")}")

    // Machine Learning
    includeDependency("ai.djl:api:${project.property("djl_version")}")
    includeDependency("ai.djl.pytorch:pytorch-engine:${project.property("djl_version")}")
//    runtimeOnly("ai.djl.mxnet:mxnet-engine:${project.property("djl_version")}")
//    runtimeOnly("ai.djl.tensorflow:tensorflow-engine:${project.property("djl_version")}")

    // HTTP library
    includeDependency("com.squareup.okhttp3:okhttp:${project.property("okhttp_version")}")
    includeDependency("com.squareup.okhttp3:okhttp-coroutines:${project.property("okhttp_version")}")

    // SOCKS5 & HTTP Proxy Support
    includeDependency("io.netty:netty-handler-proxy:${project.property("netty_version")}")

    // Update Checker
    includeDependency("com.vdurmont:semver4j:3.1.0")

    // Name Protect
    includeDependency("org.ahocorasick:ahocorasick:0.6.3")

    // Kotlin add-on for Java library
    includeDependency("net.ccbluex:fastutil-kt-ext:0.1.3")

    // Test libraries
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${project.property("kotlinx_coroutines_version")}")
//    testImplementation("net.fabricmc:fabric-loader-junit:${project.property("loader_version")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Fix nullable annotations
    compileOnlyApi("com.google.code.findbugs:jsr305:3.0.2")

    afterEvaluate {
        includeDependency.incoming.resolutionResult.allDependencies.forEach {
            val apiDependency = dependencies.api(it.requested.toString()) {
                isTransitive = false
            }

            dependencies.include(apiDependency)
        }
    }
}

tasks.processResources {
    dependsOn("bundleTheme")

    val modVersion = providers.gradleProperty("mod_version")
    val minecraftVersion = providers.gradleProperty("minecraft_version")
    val fabricVersion = providers.gradleProperty("fabric_version")
    val loaderVersion = providers.gradleProperty("loader_version")
    val minLoaderVersion = providers.gradleProperty("min_loader_version")
    val fabricKotlinVersion = providers.gradleProperty("fabric_kotlin_version")
    val viafabricplusVersion = providers.gradleProperty("viafabricplus_version")

    val contributors = provider {
        JsonOutput.prettyPrint(
            JsonOutput.toJson(getContributors("CCBlueX", "LiquidBounce"))
        )
    }

    inputs.property("version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("fabric_version", fabricVersion)
    inputs.property("loader_version", loaderVersion)
    inputs.property("min_loader_version", minLoaderVersion)
    inputs.property("fabric_kotlin_version", fabricKotlinVersion)
    inputs.property("viafabricplus_version", viafabricplusVersion)
    inputs.property("contributors", contributors)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion.get(),
                "minecraft_version" to minecraftVersion.get(),
                "fabric_version" to fabricVersion.get(),
                "loader_version" to loaderVersion.get(),
                "min_loader_version" to minLoaderVersion.get(),
                "contributors" to contributors.get(),
                "fabric_kotlin_version" to fabricKotlinVersion.get(),
                "viafabricplus_version" to viafabricplusVersion.get()
            )
        )
    }
}

// The following code will include the theme into the build

tasks.register<NpmTask>("npmInstallTheme") {
    workingDir = file("src-theme")
    args.set(listOf("i"))
    doLast {
        logger.info("Successfully installed dependencies for theme")
    }
    inputs.files("src-theme/package.json", "src-theme/package-lock.json")
    outputs.dir("src-theme/node_modules")
}

tasks.register<NpmTask>("buildTheme") {
    dependsOn("npmInstallTheme")
    workingDir = file("src-theme")
    args.set(listOf("run", "build"))
    doLast {
        logger.info("Successfully build theme")
    }

    inputs.files(
        "src-theme/package.json",
        "src-theme/package-lock.json",
        "src-theme/bundle.cjs",
        "src-theme/rollup.config.js"
    )
    inputs.dir("src-theme/src")
    outputs.dir("src-theme/dist")
}

tasks.register<NodeTask>("bundleTheme") {
    dependsOn("buildTheme")
    workingDir = file("src-theme")
    script = file("src-theme/bundle.cjs")
    doLast {
        logger.info("Successfully attached theme to build")
    }

    // Incremental stuff
    inputs.files(
        "src-theme/package.json",
        "src-theme/package-lock.json",
        "src-theme/bundle.cjs",
        "src-theme/rollup.config.js"
    )
    inputs.dir("src-theme/src")
    inputs.dir("src-theme/public")
    inputs.dir("src-theme/dist")
    outputs.files("src-theme/resources/assets/liquidbounce/themes/liquidbounce.zip")
}

sourceSets {
    main {
        resources {
            srcDirs("src-theme/resources")
        }
    }
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"

    // Minecraft 1.21.1 upwards uses Java 21.
    options.release = 21
}

tasks.test {
    useJUnitPlatform()
}

// Detekt check

detekt {
    config.setFrom(file("${rootProject.projectDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("${rootProject.projectDir}/config/detekt/baseline.xml")
}

tasks.register<DetektCreateBaselineTask>("detektProjectBaseline") {
    description = "Overrides current baseline."
    ignoreFailures.set(true)
    parallel.set(true)
    buildUponDefaultConfig.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")
}

// i18n check

tasks.register<CompareJsonKeysTask>("verifyI18nJsonKeys") {
    val baselineFileName = "en_us.json"

    group = "verification"
    description = "Compare i18n JSON files with $baselineFileName as the baseline and report missing keys."

    val languageFolder = file("src/main/resources/resources/liquidbounce/lang")
    baselineFile.set(languageFolder.resolve(baselineFileName))
    files.from(languageFolder.listFilesOrdered { it.extension.equals("json", ignoreCase = true) })
    consoleOutputCount.set(5)
}

tasks.register<JavaExec>("liquidInstruction") {
    group = "other"
    description = "Run LiquidInstruction class."

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.ccbluex.liquidbounce.LiquidInstruction")
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        suppressWarnings = true
        jvmToolchain(21)
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.jar {
    val archivesBaseName = providers.gradleProperty("archives_base_name")
    val modVersion = providers.gradleProperty("mod_version")
    val mavenGroup = providers.gradleProperty("maven_group")
    val mappingFiles = provider {
        rootProject.configurations.mappings.get().map(::zipTree)
    }

    inputs.property("archives_base_name", archivesBaseName)
    inputs.property("mod_version", modVersion)
    inputs.property("maven_group", mavenGroup)
    inputs.files(mappingFiles).withPropertyName("mappingFiles")

    manifest {
        attributes["Main-Class"] = "net.ccbluex.liquidbounce.LiquidInstruction"
        attributes["Implementation-Title"] = archivesBaseName.get()
        attributes["Implementation-Version"] = modVersion.get()
        attributes["Implementation-Vendor"] = mavenGroup.get()
    }

    // Rename the project's license file to LICENSE_<project_name> to avoid conflicts
    from("LICENSE") {
        rename {
            "${it}_${archivesBaseName.get()}"
        }
    }

    from(files(mappingFiles.get())) {
        include("mappings/mappings.tiny")
    }
}

tasks.register<Copy>("copyZipInclude") {
    from("zip_include/")
    into("build/libs/zip")
}

tasks.named("sourcesJar") {
    dependsOn("bundleTheme")
}

tasks.named("build") {
    dependsOn("copyZipInclude")
}
