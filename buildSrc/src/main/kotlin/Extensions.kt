import groovy.json.JsonSlurper
import org.gradle.api.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

fun Project.getContributors(repoOwner: String, repoName: String): List<String> = try {
    val githubToken: String? = System.getenv("GITHUB_TOKEN")

    val request = HttpRequest.newBuilder(URI("https://api.github.com/repos/${repoOwner}/${repoName}/contributors?per_page=200"))
        .GET()
        .timeout(Duration.ofSeconds(5))
        .header("User-Agent", "LiquidBounce-App")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("Accept", "application/vnd.github+json")
        .apply {
            if (!githubToken.isNullOrBlank())
                header("Authorization", "Bearer $githubToken")
        }
        .build()

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

    if (response.statusCode() in 200..299) {
        try {
            @Suppress("UNCHECKED_CAST")
            response.body().reader().use { reader ->
                (JsonSlurper().parse(reader) as List<Any?>)
                    .map { (it as? Map<String, Any?>)?.get("login") as? String }
                    .filter { !it.isNullOrBlank() && !it.contains("[bot]") } as List<String>
            }
        } catch (e: Exception) {
            logger.error("Failed to parse GitHub API response for $repoOwner:$repoName", e)
            emptyList()
        }
    } else {
        logger.error("Failed to get GitHub API response for $repoOwner:$repoName (HTTP ${response.statusCode()})", response.body().reader().readText())
        emptyList()
    }
} catch (e: Exception) {
    logger.error("Failed to fetch contributors of $repoOwner:$repoName", e)
    emptyList()
}

