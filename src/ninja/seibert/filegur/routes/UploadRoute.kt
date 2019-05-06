package ninja.seibert.filegur.routes

import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.request.receiveMultipart
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.*
import ninja.seibert.filegur.services.DownloadFileService
import ninja.seibert.filegur.services.GroupService
import ninja.seibert.filegur.services.UserPrincipal

fun Route.upload(
    groupService: GroupService,
    downloadFileService: DownloadFileService
) {
    get("/upload") {
        call.respondHtml {
            head {
                title("Upload file - Filegur")
            }
            body {
                h2 { +"Upload File" }

                form(
                    "/upload",
                    classes = "pure-form-stacked",
                    encType = FormEncType.multipartFormData,
                    method = FormMethod.post
                ) {
                    acceptCharset = "utf-8"

                    fileInput { name = "file" }
                    br()

                    submitInput(classes = "pure-button pure-button-primary") { value = "Upload" }
                }
            }
        }
    }
    post("/upload") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        val multipart = call.receiveMultipart()
        val downloadCount = downloadFileService.getAllFilesFrom(principal.user).count()
        if (group.maxFiles != -1 && downloadCount >= group.maxFiles) {
            call.respondText("You reached the group file limit!", status = HttpStatusCode.BadRequest)
            return@post
        }
        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                if (group.maxFileSize > -1 && group.maxFileSize < part.headers[HttpHeaders.ContentLength]?.toInt() ?: 0) {
                    call.respondText(
                        "The file is bigger than the group filesize limit (${group.maxFileSize})!",
                        status = HttpStatusCode.BadRequest
                    )
                    return@forEachPart
                }
                val downloadFile = downloadFileService.addFile(principal.user, part.originalFileName!!, part)
                if (downloadFile == null) {
                    call.respondText(
                        "The file is bigger than the group filesize limit (${group.maxFileSize})!",
                        status = HttpStatusCode.BadRequest
                    )
                    return@forEachPart
                }
                val url =
                    call.request.origin.run { "$scheme://$host${if (port != 443 && port != 80) ":$port" else ""}" } + "/download/" + downloadFile.obfuscatedName
                call.respondHtml {
                    head {
                        title("Upload file success - Filegur")
                    }
                    body {
                        +"Your downloadlink is "
                        a {
                            href = url
                            +url
                        }
                    }
                }
                return@forEachPart
            }
        }
    }
}