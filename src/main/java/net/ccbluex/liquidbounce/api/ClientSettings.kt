package net.ccbluex.liquidbounce.api

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage

import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import java.text.SimpleDateFormat
import kotlin.concurrent.thread

// Define a loadingLock object to synchronize access to the settings loading code
private val loadingLock = Object()

// Define a mutable list of AutoSetting objects to store the loaded settings
var autoSettingFiles: MutableList<AutoSetting>? = null

// Define a function to load settings from a remote GitHub repository
fun loadSettings(useCached: Boolean, join: Long? = null, callback: (List<AutoSetting>) -> Unit) {
    // Spawn a new thread to perform the loading operation
    val thread = thread {
        // Synchronize access to the loading code to prevent concurrent loading of settings
        synchronized(loadingLock) {
            // If cached settings are requested and have been loaded previously, return them immediately
            if (useCached && autoSettingFiles != null) {
                callback(autoSettingFiles!!)
                return@thread
            }

            try {
                // Retrieve a list of GitHubContent objects representing the files in the settings directory
                val arrayOfGitHubContent = PRETTY_GSON.fromJson(get("https://api.github.com/repos/CCBlueX/LiquidCloud/contents/LiquidBounce/settings"), Array<GitHubContent>::class.java)

                // Create a mutable list to store the parsed AutoSetting objects
                val autoSettings = mutableListOf<AutoSetting>()

                // Iterate over each GitHubContent object and parse it into an AutoSetting object
                for (content in arrayOfGitHubContent) {
                    // Retrieve the latest commit for the current file

                    // todo: figure out another way to get the last modified date without requesting the entire commit history for each file
                    // val latestCommit = PRETTY_GSON.fromJson(get("https://api.github.com/repos/CCBlueX/LiquidCloud/commits?path=${content.path}&page=1&per_page=1"), Array<GitHubCommit>::class.java).firstOrNull()

                    // If a latest commit exists, parse the date string into a Date object using SimpleDateFormat
                    // val lastModified = latestCommit?.commit?.author?.date?.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(it) }

                    // lastModified?.toString() ?: "Unknown"

                    // Create a new AutoSetting object with the parsed information and add it to the list
                    autoSettings.add(AutoSetting(content.name, "Unknown", content.downloadUrl))
                }

                // Invoke the callback with the parsed AutoSetting objects and store them in the cache for future use
                callback(autoSettings)
                autoSettingFiles = autoSettings
            } catch (e: Exception) {
                // If an error occurs, display an error message to the user
                displayChatMessage("Failed to fetch auto settings list.")
            }
        }
    }

    // If a join time is provided, block the current thread until the loading thread completes or the timeout is reached
    if (join != null) {
        thread.join(join)
    }
}

// Define a data class to represent a parsed AutoSetting object
data class AutoSetting(
    val name: String, // The name of the AutoSetting file
    val lastModified: String, // The last modification date of the AutoSetting file in string format
    val url: String // The download URL of the AutoSetting file
)

// Define a data class to represent a GitHubContent object returned by the GitHub API
data class GitHubContent(
    val name: String, // The name of the content file
    val path: String, // The path of the content file in the repository
    val sha: String, // The SHA value of the content file
    val size: Int, // The size of the content file in bytes
    val url: String, // The URL of the content file
    @SerializedName("html_url")
    val htmlUrl: String, // The HTML URL of the content file
    @SerializedName("git_url")
    val gitUrl: String, // The Git URL of the content file
    @SerializedName("download_url")
    val downloadUrl: String, // The download URL of the content file
    val type: String, // The type of the content file
    @SerializedName("_links")
    val gitHubContentLinks: GitHubContentLinks // The links to the content file
)

// Define a data class to represent the links to a GitHubContent object
data class GitHubContentLinks(
    val git: String, // The Git URL of the content file
    val self: String, // The URL of the content file
    val html: String // The HTML URL of the content file
)

// Define a data class to represent a GitHubCommit object returned by the GitHub API
data class GitHubCommit(
    val commit: GitHubCommitDetails // The details of the commit
)

// Define a data class to represent the details of a GitHubCommit object
data class GitHubCommitDetails(
    val author: GitHubCommitAuthor, // The author of the commit
    val message: String // The message of the commit
)

// Define a data class to represent the author of a GitHubCommit object
data class GitHubCommitAuthor(
    val name: String, // The name of the commit author
    val email: String, // The email of the commit author
    val date: String // The date of the commit in string format
)