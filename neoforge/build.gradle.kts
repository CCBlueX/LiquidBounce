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

plugins {
    `java-library`
    alias(libs.plugins.moddev)
    alias(libs.plugins.kotlin.jvm)
}

base {
    archivesName = "${rootProject.property("archives_base_name")}-neoforge"
    version = rootProject.property("mod_version") as String
    group = rootProject.property("maven_group") as String
}

/** Includes dependency recursively in the JAR file (NeoForge jarJar) */
val jij: Configuration by configurations.creating

jij.excludeProvidedLibs()

/**
 * Kotlin runtime bundled into the jar. On Fabric it is provided by the
 * fabric-language-kotlin mod; NeoForge has no equivalent dependency, so the
 * runtime is shipped the same way KotlinForForge does (nested library jars).
 * Kept separate from [jij] because [excludeProvidedLibs] excludes it there.
 */
val kotlinRuntime: Configuration by configurations.creating

// Reuses the loader-agnostic sources of the root (Fabric) project. The Fabric-specific
// sources live in src/fabric and are not part of this source set; this module provides
// its own platform implementation instead.
sourceSets {
    main {
        java.srcDir(rootProject.file("src/main/java"))
        kotlin.srcDir(rootProject.file("src/main/kotlin"))
        resources.srcDirs(
            rootProject.file("src/main/resources"),
            rootProject.file("src-theme/resources")
        )
    }
}

val convertAccessWidener = tasks.register<ConvertAccessWidenerTask>("convertAccessWidener") {
    description = "Generates the NeoForge access transformer from the shared access widener."

    accessWidener = rootProject.file("src/main/resources/liquidbounce.accesswidener")
    // The root project's Loom-provided Minecraft jar uses the same Mojang mappings
    // as NeoForge and provides the class hierarchy for override propagation.
    hierarchyClasspath.from(
        (rootProject.extensions.getByName("loom") as net.fabricmc.loom.api.LoomGradleExtensionAPI)
            .namedMinecraftJars
    )
    output = layout.buildDirectory.file("generated/accessTransformer/accesstransformer.cfg")
}

neoForge {
    version = libs.versions.neoforge.get()

    accessTransformers.from(convertAccessWidener.map { it.output })

    runs {
        register("client") {
            client()
        }
    }

    mods {
        register("liquidbounce") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    // ViaFabricPlus API - compile-time only; all usage sites are guarded by
    // Platform.isModLoaded("viafabricplus"), which is always false on NeoForge.
    compileOnly(libs.vfp.api)

    // Exploit Preventer API - compile-time only, guarded by Class.forName checks.
    compileOnly(libs.exploitPreventer.api)

    // Mixin targets of the mod-compat mixins, which are inert at runtime unless
    // the target classes are present. Sodium and Lithium are multiloader, so the
    // Fabric artifacts provide the correct classes to compile against.
    compileOnly(libs.sodium)
    compileOnly(libs.lithium)

    // JCEF Support - compile-time until the MCEF NeoForge artifact is available;
    // without it at runtime, the browser backend is skipped (LB_BROWSER_SKIP behavior).
    compileOnly(libs.mcef)

    // Minecraft Authlib
    jij(libs.mcAuthlib)

    // LWJGL EGL
    jij(libs.lwjgl.egl)

    jij(libs.httpServer)

    // Discord RPC Support
    jij(libs.discordIpc)

    // ScriptAPI
    jij(libs.polyglot)
    jij(libs.polyglot.js)
    jij(libs.polyglot.tools)

    // Machine Learning
    jij(libs.djl.api)
    jij(libs.djl.pytorch)

    // HTTP library
    jij(libs.bundles.okhttp)

    // SOCKS5 & HTTP Proxy Support
    jij(libs.netty.handler.proxy)

    // Update Checker
    jij(libs.semver4j)

    // Name Protect
    jij(libs.ahocorasick)

    // External utils
    compileOnlyApi(libs.fastutil4k.extensionsOnly)
    jij(libs.fastutil4k.moreCollections)

    // Kotlin runtime
    kotlinRuntime(libs.kotlin.stdlib)
    kotlinRuntime(libs.kotlin.reflect)
    kotlinRuntime(libs.kotlinx.coroutines.core)
    kotlinRuntime(libs.kotlinx.coroutines.jdk8)
    kotlinRuntime(libs.kotlinx.atomicfu)
    kotlinRuntime(libs.kotlinx.datetime)
    kotlinRuntime(libs.kotlinx.io.core)
    kotlinRuntime(libs.kotlinx.io.bytestring)
    kotlinRuntime(libs.kotlinx.serialization.core)
    kotlinRuntime(libs.kotlinx.serialization.json)
    kotlinRuntime(libs.kotlinx.serialization.cbor)
}

addResolvedDependencies(jij, "compileOnly", "jarJar", "api")
addResolvedDependencies(kotlinRuntime, "compileOnly", "jarJar", "runtimeOnly")

tasks.processResources {
    dependsOn(rootProject.tasks.named("bundleTheme"))

    // Fabric-only metadata; the equivalent access transformer is generated by convertAccessWidener
    exclude("liquidbounce.accesswidener")

    from(convertAccessWidener.map { it.output }) {
        into("META-INF")
    }

    val modVersion = rootProject.providers.gradleProperty("mod_version")
    val minecraftVersionRange = libs.versions.minecraft.map { "[$it,)" }
    val neoforgeVersionRange = libs.versions.neoforge.map { "[$it,)" }

    inputs.property("version", modVersion)
    inputs.property("minecraft_version_range", minecraftVersionRange)
    inputs.property("neoforge_version_range", neoforgeVersionRange)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            mapOf(
                "version" to modVersion.get(),
                "minecraft_version_range" to minecraftVersionRange.get(),
                "neoforge_version_range" to neoforgeVersionRange.get()
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.jdk.get().toInt()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

kotlin {
    compilerOptions {
        suppressWarnings = true
        jvmToolchain(libs.versions.jdk.get().toInt())
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.jar {
    val archivesBaseName = rootProject.providers.gradleProperty("archives_base_name")
    val modVersion = rootProject.providers.gradleProperty("mod_version")
    val mavenGroup = rootProject.providers.gradleProperty("maven_group")

    inputs.property("archives_base_name", archivesBaseName)
    inputs.property("mod_version", modVersion)
    inputs.property("maven_group", mavenGroup)

    manifest {
        attributes["Main-Class"] = "net.ccbluex.liquidbounce.LiquidInstruction"
        attributes["Implementation-Title"] = archivesBaseName.get()
        attributes["Implementation-Version"] = modVersion.get()
        attributes["Implementation-Vendor"] = mavenGroup.get()
    }

    from(rootProject.file("LICENSE")) {
        rename {
            "${it}_${archivesBaseName.get()}"
        }
    }
}
