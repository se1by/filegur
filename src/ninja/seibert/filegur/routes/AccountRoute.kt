package ninja.seibert.filegur.routes

import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.*
import ninja.seibert.filegur.services.DownloadFileService
import ninja.seibert.filegur.services.DownloadService
import ninja.seibert.filegur.services.UserPrincipal
import ninja.seibert.filegur.services.UserService
import ninja.seibert.filegur.utils.sha512
import ninja.seibert.filegur.utils.toLocalDateTimeString

fun Route.account(
    userService: UserService,
    downloadFileService: DownloadFileService,
    downloadService: DownloadService
) {
    get("/account") {
        val principal = call.principal<UserPrincipal>()
        val downloadFiles = downloadFileService.getAllFilesFrom(principal!!.user)
        call.respondHtml {
            head {
                styleLink("/css/main.css")
                title("Account - Filegur")
            }
            body {
                h2 { +"Hello ${principal.user.name}" }
                table {
                    tr {
                        th { +"Uploaded" }
                        th { +"Filename" }
                        th { +"Obfuscated name" }
                        th { +"Downloads" }
                        th { +"Link" }
                    }
                    downloadFiles.forEach {
                        tr {
                            td { +it.uploadDate.toLocalDateTimeString() }
                            td { +it.originalName }
                            td { +it.obfuscatedName }
                            val downloads = downloadService.getAllDownloads(it)
                            td {
                                if (downloads.isNotEmpty()) {
                                    a {
                                        href = "/statistics/${it.obfuscatedName}"
                                        +"${downloads.size}"
                                    }
                                } else {
                                    +"${downloads.size}"
                                }
                            }
                            val url =
                                call.request.origin.run { "$scheme://$host${if (port != 443 && port != 80) ":$port" else ""}" } + "/" + it.obfuscatedName
                            td {
                                a {
                                    href = url
                                    +"Link"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    get("/account/settings") {
        call.respondHtml {
            head {
                title("Account settings - Filegur")
            }
            body {
                h2 { +"Change password" }
                form(
                    "/account/settings/changePassword",
                    encType = FormEncType.applicationXWwwFormUrlEncoded,
                    method = FormMethod.post
                ) {
                    label {
                        htmlFor = "oldPassword"; +"Old password: "
                        passwordInput { name = "oldPassword"; id = "oldPassword" }
                    }
                    br { }
                    br { }
                    label {
                        htmlFor = "newPassword"; +"New password: "
                        passwordInput { name = "newPassword"; id = "newPassword" }
                    }
                    br { }
                    br { }
                    submitInput { value = "Change" }
                }
            }
        }
    }

    post("/account/settings/changePassword") {
        val parameters = call.receiveParameters()
        val oldPassword = parameters["oldPassword"]
        if (oldPassword == null) {
            call.respondText("You didn't enter your old password!", status = HttpStatusCode.BadRequest)
            return@post
        }
        val newPassword = parameters["newPassword"]
        if (newPassword == null) {
            call.respondText("You didn't enter your new password!", status = HttpStatusCode.BadRequest)
            return@post
        }

        val principal = call.principal<UserPrincipal>()!!
        if (sha512(principal.user.salt + oldPassword) != principal.user.passwordHash) {
            call.respondText("You old password was not correct!", status = HttpStatusCode.BadRequest)
            return@post
        }
        userService.updateUser(
            principal.user.id, principal.user.name, principal.user.salt,
            sha512(principal.user.salt + newPassword)
        )
        call.respondText("Your password was updated.")
    }
}