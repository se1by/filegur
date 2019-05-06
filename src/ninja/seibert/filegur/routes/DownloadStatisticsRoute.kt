package ninja.seibert.filegur.routes

import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.html.*
import ninja.seibert.filegur.services.DownloadFileService
import ninja.seibert.filegur.services.DownloadService
import ninja.seibert.filegur.services.GroupService
import ninja.seibert.filegur.services.UserPrincipal
import ninja.seibert.filegur.utils.toLocalDateTimeString

fun Route.downloadStatistics(
    groupService: GroupService,
    downloadFileService: DownloadFileService,
    downloadService: DownloadService
) {
    get("/statistics/{obfuscatedName}") {
        val principal = call.principal<UserPrincipal>()!!
        val obfuscatedName = call.parameters["obfuscatedName"] ?: ""
        val downloadFile = downloadFileService.getFile(obfuscatedName)
        if (downloadFile == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        if (downloadFileService.getOwner(downloadFile).id != principal.user.id
            && !groupService.getGroup(principal.user).canViewAllDownloadStatistics
        ) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val downloads = downloadService.getAllDownloads(downloadFile)
        call.respondHtml {
            head {
                styleLink("/css/main.css")
                title("Download statistics - ${downloadFile.originalName} - Filegur")
            }
            body {
                h2 { +"${downloadFile.originalName} (${downloadFile.uploadDate.toLocalDateTimeString()})" }
                br { }
                table {
                    tr {
                        th { +"Date" }
                        th { +"IP Address" }
                        th { +"User Agent" }
                    }
                    downloads.forEach {
                        tr {
                            td { +it.datetime.toLocalDateTimeString() }
                            td { +it.ipAddress }
                            td { +it.userAgent }
                        }
                    }
                }
            }
        }
    }
}