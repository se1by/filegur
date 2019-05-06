package ninja.seibert.filegur.services

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class DownloadService {
    fun addDownload(downloadFileId: DownloadFile, ipAddress: String, userAgent: String) {
        transaction {
            Download.new {
                file = downloadFileId
                datetime = DateTime.now()
                this.ipAddress = ipAddress
                this.userAgent = userAgent
            }
        }
    }

    fun getAllDownloads(downloadFile: DownloadFile): List<Download> {
        val downloads = mutableListOf<Download>()
        transaction {
            downloads.addAll(Download.find { Downloads.file eq downloadFile.id })
        }
        return downloads
    }
}

object Downloads : IntIdTable() {
    val file = reference("file", DownloadFiles)
    val datetime = datetime("datetime")
    val ipAddress = varchar("ip_address", 45)
    val userAgent = varchar("user_gent", 256)
}

class Download(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Download>(Downloads)

    var file by DownloadFile referencedOn Downloads.file
    var datetime by Downloads.datetime
    var ipAddress by Downloads.ipAddress
    var userAgent by Downloads.userAgent
}