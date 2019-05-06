package ninja.seibert.filegur.routes

import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.*
import ninja.seibert.filegur.services.DownloadFileService
import ninja.seibert.filegur.services.GroupService
import ninja.seibert.filegur.services.UserPrincipal
import ninja.seibert.filegur.services.UserService
import ninja.seibert.filegur.utils.randomHexChars
import ninja.seibert.filegur.utils.sha512

fun Route.users(userService: UserService, groupService: GroupService, downloadFileService: DownloadFileService) {
    get("/admin/users") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        if (!group.canCreateUsers) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val users = userService.getAllUsers()
        call.respondHtml {
            head {
                styleLink("/css/main.css")
                title("User management - Filegur")
            }
            body {
                h2 { +"Users" }
                form { action = "/admin/users/create"; method = FormMethod.get; submitInput { value = "Create" } }
                table {
                    tr {
                        th { +"Name" }
                        th { +"Group" }
                        th { +"Uploaded files" }
                        th { +"Total downloads" }
                    }
                    users.forEach {
                        tr {
                            td { +it.name }
                            val group1 = groupService.getGroup(it)
                            td { +group1.name }
                            val downloadFiles = downloadFileService.getAllFilesFrom(it)
                            td { +downloadFiles.size.toString() }
                            td { +downloadFiles.map { downloadFileService.getDownloads(it).size }.sum().toString() }
                        }
                    }
                }
            }
        }
    }

    get("/admin/users/create") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        if (!group.canCreateUsers) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val allGroups = groupService.getAllGroups()
        call.respondHtml {
            head {
                title("Create user - Filegur")
            }
            body {
                h2 { +"Create user" }
                form {
                    action = "/admin/users/create"
                    method = FormMethod.post
                    encType = FormEncType.applicationXWwwFormUrlEncoded

                    label {
                        htmlFor = "name"; +"Name: "
                        textInput { name = "name"; id = "name" }
                    }
                    br { }
                    label {
                        htmlFor = "group"; +"Group: "
                        select {
                            name = "group"; id = "group"
                            allGroups.forEach {
                                option {
                                    label = it.name
                                    value = it.id.value.toString()
                                    +it.name
                                }
                            }
                        }
                    }
                    br { }
                    label {
                        htmlFor = "password"; +"Password: "
                        passwordInput { name = "password"; id = "password" }
                    }
                    br { }
                    submitInput { value = "Create" }
                }
            }
        }
    }

    post("/admin/users/create") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        if (!group.canCreateUsers) {
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }

        val parameters = call.receiveParameters()
        val name = parameters["name"]
        if (name == null) {
            call.respondText("Missing name!", status = HttpStatusCode.BadRequest)
            return@post
        }
        val userGroup = parameters["group"]
        if (userGroup == null) {
            call.respondText("Missing group!", status = HttpStatusCode.BadRequest)
            return@post
        }
        val password = parameters["password"]
        if (password == null) {
            call.respondText("Missing password!")
            return@post
        }
        val salt = randomHexChars(16)
        userService.createUser(name, salt, sha512(salt + password), groupService.getGroupById(userGroup.toInt())!!)
        call.respondRedirect("/admin/users", false)
    }
}