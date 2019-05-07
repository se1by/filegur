package ninja.seibert.filegur

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import ninja.seibert.filegur.routes.*
import ninja.seibert.filegur.services.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    initDB()

    // config values
    val gdprNoticeProvided = environment.config.propertyOrNull("filegur.gdprNoticeProvided")?.getString()?.toBoolean() ?: false
    val useForwardedHeaders = environment.config.propertyOrNull("filegur.useForwardedHeaders")?.getString()?.toBoolean() ?: false

    val userService = UserService()
    val groupService = GroupService()
    val downloadFileService = DownloadFileService()
    val downloadService = DownloadService()
    val requestLoggingService = RequestLoggingService()

    if (useForwardedHeaders) {
        install(XForwardedHeaderSupport)
    }

    install(Authentication) {
        basic {
            realm = "Filegur"
            validate {
                userService.getPrincipal(it)
            }
        }
    }

    if (gdprNoticeProvided) {
        intercept(ApplicationCallPipeline.Monitoring) {
            proceed()
            requestLoggingService.logRequest(call)
        }
    }

    routing {
        download(downloadFileService, downloadService, gdprNoticeProvided)

        static {
            resource("favicon.ico")
            resource("css/main.css")
        }

        authenticate {
            upload(groupService, downloadFileService)
            account(userService, downloadFileService, downloadService)
            downloadStatistics(groupService, downloadFileService, downloadService)
            requestLog(groupService, requestLoggingService)
            users(userService, groupService, downloadFileService)
            groups(groupService)
        }
    }
}

fun initDB() {
    Database.connect("jdbc:sqlite:filegur.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Users,
            Groups,
            Downloads,
            DownloadFiles,
            Requests
        )
        val group = if (Group.count() == 0) {
            Group.new {
                name = "admin"
                canCreateUsers = true
                canCreateGroups = true
                canViewAllDownloadStatistics = true
                canViewRequestLog = true
                maxFiles = -1
                maxFileSize = -1
            }
        } else {
            Group.all().first()
        }

        if (Users.selectAll().count() == 0) {
            User.new {
                name = "admin"
                salt = "b041b82049b4c293d5f2a65deabfae9b"
                passwordHash =
                    "674C35AC73A610601032A615B79A43345024D61AC4B28C4F83096721D39862BA56C2F995A5D1995F18333440DE325EC908675B42489307569BF9DC7FEAC07EF3"
                this.group = group
            }
        }
    }
}

