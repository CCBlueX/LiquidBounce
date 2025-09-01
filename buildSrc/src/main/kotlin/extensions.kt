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
import groovy.json.JsonSlurper
import org.gradle.api.Task
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
            if (!githubToken.isNullOrBlank())
                header("Authorization", "Bearer $githubToken")
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
                    logger.error("Failed to get GitHub API response for $repoOwner:$repoName (HTTP ${response.statusCode()}): ${response.body().bufferedReader().readText()}")
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
