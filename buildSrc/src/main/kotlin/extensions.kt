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

import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.regex.Pattern

private val httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build()

private inline val HttpResponse<*>.isSuccessful get() = statusCode() in 200..299

/**
 * [API Docs](https://docs.github.com/zh/rest/collaborators/collaborators?apiVersion=2022-11-28)
 */
fun Task.getContributors(repoOwner: String, repoName: String): List<String> = try {
    val githubToken: String? = System.getenv("GITHUB_TOKEN")

    fun HttpRequest.Builder.generalSettings() = this
        .timeout(Duration.ofSeconds(10))
        .header("User-Agent", "LiquidBounce-App")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("Accept", "application/vnd.github+json")
        .apply {
            if (!githubToken.isNullOrBlank()) {
                header("Authorization", "Bearer $githubToken")
            }
        }

    fun HttpClient.fetchLastPage(baseUrl: String, perPage: Int): Int {
        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl?per_page=$perPage"))
            .HEAD()
            .generalSettings()
            .build()

        val response = send(request, HttpResponse.BodyHandlers.discarding())

        return if (response.isSuccessful) {
            val linkHeader = response.headers().firstValue("link").orElse("")
            val pattern = Pattern.compile("&page=(\\d+)>; rel=\"last\"")
            val matcher = pattern.matcher(linkHeader)
            return if (matcher.find()) matcher.group(1).toInt() else 1
        } else {
            logger.error("HEAD request to ${response.uri()} failed with status: ${response.statusCode()}")
            1
        }
    }

    val baseUrl = "https://api.github.com/repos/${repoOwner}/${repoName}/contributors"

    val perPage = 100 // Maximum is 100
    val maxPage = httpClient.fetchLastPage(baseUrl, perPage)

    (1..maxPage).map { page ->
        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl?per_page=$perPage&page=$page"))
            .GET()
            .generalSettings()
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApply { response ->
                if (response.isSuccessful) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        response.body().use { inputStream ->
                            (JsonSlurper().parse(inputStream) as List<Map<String, Any?>>)
                                .mapNotNull {
                                    if (it["type"] == "User") it["login"] as String else null
                                }
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to parse GitHub API response for $repoOwner:$repoName", e)
                        emptyList()
                    }
                } else {
                    logger.error("Failed to get GitHub API response for $repoOwner:$repoName " +
                        "(HTTP ${response.statusCode()}): ${response.body().bufferedReader().readText()}")
                    emptyList()
                }
            }
    }.flatMapTo(ArrayList(perPage * maxPage)) {
        it.get()
    }.also {
        logger.info("Successfully collected ${it.size} contributors")
    }
} catch (e: Exception) {
    logger.error("Failed to fetch contributors of $repoOwner:$repoName", e)
    emptyList()
}

fun Project.addResolvedDependencies(
    from: Configuration,
    vararg toConfigurations: String,
) {
    val resolvedDeps = from.incoming.resolutionResult.allDependencies
        .map { dep ->
            val requested = dep.requested.displayName
            dependencies.create(requested) {
                (this as? ModuleDependency)?.isTransitive = false
            }
        }

    toConfigurations.forEach { configName ->
        configurations.named(configName).configure {
            withDependencies {
                addAll(resolvedDeps)
            }
        }
    }
}

/**
 * Provided by:
 * - Minecraft
 * - Mod dependencies
 */
fun Configuration.excludeProvidedLibs() = apply {
    // fabric-language-kotlin
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    exclude(group = "org.jetbrains.kotlinx", module = "atomicfu")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-datetime")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-bytestring")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-cbor")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")

    // Minecraft
    exclude(group = "it.unimi.dsi", module = "fastutil")
    exclude(group = "com.google.guava", module = "guava")
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
    exclude(group = "org.lwjgl", module = "lwjgl")

    exclude(group = "io.netty", module = "netty-buffer")
    exclude(group = "io.netty", module = "netty-codec")
    exclude(group = "io.netty", module = "netty-codec-base")
    exclude(group = "io.netty", module = "netty-codec-compression")
    exclude(group = "io.netty", module = "netty-codec-http")
    exclude(group = "io.netty", module = "netty-common")
    exclude(group = "io.netty", module = "netty-handler")
    exclude(group = "io.netty", module = "netty-resolver")
    exclude(group = "io.netty", module = "netty-transport")
    exclude(group = "io.netty", module = "netty-transport-classes-epoll")
    exclude(group = "io.netty", module = "netty-transport-classes-kqueue")
    exclude(group = "io.netty", module = "netty-transport-native-epoll")
    exclude(group = "io.netty", module = "netty-transport-native-kqueue")
    exclude(group = "io.netty", module = "netty-transport-native-unix-common")
}
