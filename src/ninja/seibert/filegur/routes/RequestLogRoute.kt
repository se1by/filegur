package ninja.seibert.filegur.routes

import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.html.*
import ninja.seibert.filegur.services.GroupService
import ninja.seibert.filegur.services.RequestLoggingService
import ninja.seibert.filegur.services.UserPrincipal
import ninja.seibert.filegur.utils.toLocalDateTimeString

fun Route.requestLog(
    groupService: GroupService,
    requestLoggingService: RequestLoggingService
) {
    get("/admin/requestlog") {
        val principal = call.principal<UserPrincipal>()!!
        val group = groupService.getGroup(principal.user)
        if (!group.canViewRequestLog) {
            call.respond(HttpStatusCode.Forbidden)
            return@get
        }
        val showResources = call.parameters["showResources"]?.toBoolean() ?: false
        call.respondHtml {
            head {
                styleLink("/css/main.css")
                title("Request log - Filegur")
            }
            body {
                h2 { +"Latest 10 requests:" }
                label {
                    htmlFor = "showResources"; +"Show resources"
                    checkBoxInput {
                        name = "showResources"; id = "showResources"
                        checked = showResources
                        onChange =
                            "(function(e) { window.location.href='//' + window.location.host + window.location.pathname + (e.checked ? '?showResources=true' : '');})(this)"
                    }
                }
                table {
                    tr {
                        th { +"RequestDate" }
                        th { +"Method" }
                        th { +"URI" }
                        th { +"Status" }
                        th { +"IP Address" }
                        th { +"Username" }
                        th { +"User Agent" }
                    }
                    val requests = requestLoggingService.getLatestRequests(
                        10,
                        ignoreResources = !showResources
                    )
                    requests.forEach {
                        tr {
                            td { +it.datetime.toLocalDateTimeString() }
                            td { +it.method }
                            td { +it.uri }
                            td { +it.status }
                            td { +it.ipAddress }
                            td { +(it.user ?: "") }
                            td { +it.userAgent }
                        }
                    }
                }
            }
        }
    }
}