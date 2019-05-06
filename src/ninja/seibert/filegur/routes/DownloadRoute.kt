package ninja.seibert.filegur.routes

import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.request.userAgent
import io.ktor.response.header
import io.ktor.response.respondFile
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import ninja.seibert.filegur.services.DownloadFileService
import ninja.seibert.filegur.services.DownloadService
import java.io.File

fun Route.download(downloadFileService: DownloadFileService, downloadService: DownloadService, trackDownloads: Boolean = false) {
    get("/download/{filename}") {
        val filename: String = call.parameters["filename"] ?: ""
        val downloadFile = downloadFileService.getFile(filename)
        if (downloadFile == null) {
            call.respondText("Dead link!")
            return@get
        }
        if (trackDownloads) {
            downloadService.addDownload(downloadFile, call.request.origin.remoteHost, call.request.userAgent() ?: "")
        }

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                downloadFile.originalName
            ).toString()
        )
        call.respondFile(File(downloadFile.fsPath))
    }
}