package ninja.seibert.filegur.services

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import ninja.seibert.filegur.utils.md5
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class DownloadFileService {
    fun getAllFilesFrom(user: User): List<DownloadFile> {
        val downloadfiles = mutableListOf<DownloadFile>()
        transaction {
            val files = user.downloadFiles
            downloadfiles.addAll(files)
        }
        return downloadfiles
    }

    suspend fun addFile(user: User, title: String, fileItem: PartData.FileItem): DownloadFile? {
        val filesDir = File("files")
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }

        val file = File("files" + File.separator + "${System.currentTimeMillis()}-$title")
        fileItem.streamProvider().use { its -> file.outputStream().buffered().use { its.copyToSuspend(it) } }

        var downloadFile: DownloadFile? = null
        transaction {
            if (user.group.maxFileSize > -1 && file.length() > user.group.maxFileSize) {
                file.delete()
                return@transaction
            }
            downloadFile = DownloadFile.new {
                owner = user
                originalName = title
                obfuscatedName = md5(System.currentTimeMillis().toString() + title)
                uploadDate = DateTime.now()
                fsPath = file.absolutePath
            }
        }
        return downloadFile
    }

    fun getFile(obfuscatedName: String): DownloadFile? {
        var downloadFile: DownloadFile? = null
        transaction {
            downloadFile = DownloadFile.find { DownloadFiles.obfuscatedName eq obfuscatedName }.firstOrNull()
        }
        return downloadFile
    }

    fun getOwner(downloadFile: DownloadFile): User {
        var user: User? = null
        transaction {
            user = downloadFile.owner
        }
        return user!!
    }

    fun getDownloads(downloadFile: DownloadFile): List<Download> {
        val downloads = mutableListOf<Download>()
        transaction {
            downloads.addAll(downloadFile.downloads)
        }
        return downloads
    }
}

object DownloadFiles : IntIdTable() {
    val owner = reference("owner", Users)
    val originalName = varchar("originalName", 256)
    val obfuscatedName = varchar("obfuscatedName", 32)
    val uploadDate = datetime("uploadDate")
    val fsPath = varchar("fsPath", 256)
}

class DownloadFile(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DownloadFile>(DownloadFiles)

    var owner by User referencedOn DownloadFiles.owner
    var originalName by DownloadFiles.originalName
    var obfuscatedName by DownloadFiles.obfuscatedName
    var uploadDate by DownloadFiles.uploadDate
    var fsPath by DownloadFiles.fsPath
    val downloads by Download referrersOn Downloads.file
}

suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}