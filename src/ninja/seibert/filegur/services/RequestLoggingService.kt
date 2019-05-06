package ninja.seibert.filegur.services

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.features.origin
import io.ktor.request.queryString
import io.ktor.request.uri
import io.ktor.request.userAgent
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class RequestLoggingService {
    fun logRequest(call: ApplicationCall) {
        transaction {
            Request.new {
                datetime = DateTime.now()
                method = call.request.origin.method.value
                uri = call.request.uri.removeSuffix("?${call.request.queryString()}")
                status = call.response.status().toString()
                ipAddress = call.request.origin.remoteHost
                user = call.principal<UserPrincipal>()?.user?.name
                userAgent = call.request.userAgent() ?: ""
            }
        }
    }

    fun getLatestRequests(maxRequests: Int, offset: Int = 0, ignoreResources: Boolean = false): List<Request> {
        val requests = mutableListOf<Request>()
        transaction {
            if (ignoreResources) {
                requests.addAll(
                    Request.find { (Requests.uri notLike "%.css") and (Requests.uri notLike "%.ico") and (Requests.uri notLike "%.js") }
                        .orderBy(Requests.datetime to SortOrder.DESC).limit(maxRequests, offset)
                )
            } else {
                requests.addAll(
                    Request.all().orderBy(Requests.datetime to SortOrder.DESC).limit(maxRequests, offset)
                )
            }
        }
        return requests
    }
}

object Requests : IntIdTable() {
    val datetime = datetime("datetime").index()
    val method = varchar("method", 16)
    val uri = varchar("uri", 256)
    val status = varchar("status", 64)
    val ipAddress = varchar("ip_address", 46)
    val user = varchar("username", 64).nullable()
    val userAgent = varchar("user_agent", 256)
}

class Request(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Request>(Requests)

    var datetime by Requests.datetime
    var method by Requests.method
    var uri by Requests.uri
    var status by Requests.status
    var ipAddress by Requests.ipAddress
    var user by Requests.user
    var userAgent by Requests.userAgent
}