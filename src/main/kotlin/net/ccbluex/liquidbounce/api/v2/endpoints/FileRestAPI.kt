package net.ccbluex.liquidbounce.api.v2.endpoints

import net.ccbluex.liquidbounce.api.v2.ClientApiV2
import java.io.File

class FileRestAPI(private val api: ClientApiV2) {

    /**
     * Download the file with the specified id as string.
     */
    fun download(fileId: Int) = api.request("file/download/$fileId", "GET")

    /**
     * Downloads the file with the specified id and writes it to the specified file.
     *
     * TODO: Use buffered writing instead of writing the whole file at once.
     */
    fun downloadAsFile(fileId: Int, file: File) =
        api.request("file/download/$fileId", "GET").also { file.writeText(it) }

}
