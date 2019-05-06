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
import ninja.seibert.filegur.services.GroupService
import ninja.seibert.filegur.services.UserPrincipal

fun Route.groups(groupService: GroupService) {
    get("/admin/groups") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        if (!group.canCreateGroups) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val allGroups = groupService.getAllGroups()
        call.respondHtml {
            head {
                styleLink("/css/main.css")
                title("Groups management - Filegur")
            }
            body {
                h2 { +"Groups" }
                form { action = "/admin/groups/create"; method = FormMethod.get; submitInput { value = "Create" } }
                table {
                    tr {
                        th { +"Name" }
                        th { +"Can create users" }
                        th { +"Can create groups" }
                        th { +"Can view all download statistics" }
                        th { +"Can view requestlog" }
                        th { +"Max files" }
                        th { +"Max file size" }
                    }
                    allGroups.forEach {
                        tr {
                            td { +it.name }
                            td { checkBoxInput { disabled = true; checked = it.canCreateUsers } }
                            td { checkBoxInput { disabled = true; checked = it.canCreateGroups } }
                            td { checkBoxInput { disabled = true; checked = it.canViewAllDownloadStatistics } }
                            td { checkBoxInput { disabled = true; checked = it.canViewRequestLog } }
                            td { +it.maxFiles.toString() }
                            td { +it.maxFileSize.toString() }
                        }
                    }
                }
            }
        }
    }
    get("/admin/groups/create") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        if (!group.canCreateGroups) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        call.respondHtml {
            head {
                styleLink("/css/main.css")
                title("Create group - Filegur")
            }
            body {
                h2 { +"Create group" }
                form {
                    action = "/admin/groups/create"
                    encType = FormEncType.applicationXWwwFormUrlEncoded
                    method = FormMethod.post

                    label {
                        htmlFor = "name"; +"Name:"
                        textInput { name = "name"; id = "name" }
                    }
                    br { }
                    label {
                        htmlFor = "canCreateUsers"; +"Can create users:"
                        checkBoxInput { name = "canCreateUsers"; id = "canCreateUsers" }
                    }
                    br { }
                    label {
                        htmlFor = "canCreateGroups"; +"Can create groups:"
                        checkBoxInput { name = "canCreateGroups"; id = "canCreateGroups" }
                    }
                    br { }
                    label {
                        htmlFor = "canViewAllDownloadStatistics"; +"Can view all download statistics:"
                        checkBoxInput { name = "canViewAllDownloadStatistics"; id = "canViewAllDownloadStatistics" }
                    }
                    br { }
                    label {
                        htmlFor = "canViewRequestLog"; +"Can view requestlog:"
                        checkBoxInput { name = "canViewRequestlog"; id = "canViewRequestlog" }
                    }
                    br { }
                    label {
                        htmlFor = "maxFiles"; +"Max files: "
                        numberInput { name = "maxFiles"; id = "maxFiles" }
                    }
                    br { }
                    label {
                        htmlFor = "maxFileSize"; +"Max file size: "
                        numberInput { name = "maxFileSize"; id = "maxFileSize" }
                    }
                    br { }
                    submitInput { value = "Create" }
                }
            }
        }
    }

    post("/admin/groups/create") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        if (!group.canCreateGroups) {
            call.respond(HttpStatusCode.Forbidden)
            return@post
        }
        val parameters = call.receiveParameters()
        val name = parameters["name"]
        if (name == null) {
            call.respondText("Missing name!", status = HttpStatusCode.BadRequest)
            return@post
        }
        val canCreateUsers = parameters["canCreateUsers"]?.toBoolean() ?: false
        val canCreateGroups = parameters["canCreateGroups"]?.toBoolean() ?: false
        val canViewAllDownloadStatistics = parameters["canViewAllDownloadStatistics"]?.toBoolean() ?: false
        val canViewRequestlog = parameters["canViewRequestlog"]?.toBoolean() ?: false
        val maxFiles = parameters["maxFiles"]?.toIntOrNull()
        if (maxFiles == null) {
            call.respondText("maxFiles missing or not an Int!", status = HttpStatusCode.BadRequest)
            return@post
        }
        val maxFileSize = parameters["maxFileSize"]?.toIntOrNull()
        if (maxFileSize == null) {
            call.respondText("maxFileSize missing or not an Int!", status = HttpStatusCode.BadRequest)
            return@post
        }

        groupService.createGroup(name, canCreateUsers, canCreateGroups, canViewAllDownloadStatistics, canViewRequestlog, maxFiles, maxFileSize)
        call.respondRedirect("/admin/groups", false)
    }
}