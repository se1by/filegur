package ninja.seibert.filegur.services

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

class GroupService {
    fun getGroup(user: User): Group {
        var group: Group? = null
        transaction {
            val attachedUser = User.findById(user.id)
            group = attachedUser?.group
        }
        return group!!
    }

    fun getAllGroups(): List<Group> {
        val groups = mutableListOf<Group>()
        transaction {
            groups.addAll(Group.all())
        }
        return groups
    }

    fun getGroupById(id: Int): Group? {
        return transaction {
            Group.findById(id)
        }
    }

    fun createGroup(
        name: String,
        canCreateUsers: Boolean,
        canCreateGroups: Boolean,
        canViewAllDownloadStatistics: Boolean,
        canViewRequestLog: Boolean,
        maxFiles: Int,
        maxFileSize: Int
    ): Group {
        return transaction {
            Group.new {
                this.name = name
                this.canCreateUsers = canCreateUsers
                this.canCreateGroups = canCreateGroups
                this.canViewAllDownloadStatistics = canViewAllDownloadStatistics
                this.canViewRequestLog = canViewRequestLog
                this.maxFiles = maxFiles
                this.maxFileSize = maxFileSize
            }
        }
    }
}

object Groups : IntIdTable() {
    val name = varchar("name", 64).uniqueIndex()
    val canCreateUsers = bool("can_create_users")
    val canCreateGroups = bool("can_create_groups")
    val canViewAllDownloadStatistics = bool("can_view_all_download_statistics")
    val canViewRequestLog = bool("can_view_request_log")
    val maxFiles = integer("max_files")
    val maxFileSize = integer("max_file_size")
}

class Group(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Group>(Groups)

    var name by Groups.name
    var canCreateUsers by Groups.canCreateUsers
    var canCreateGroups by Groups.canCreateGroups
    var canViewAllDownloadStatistics by Groups.canViewAllDownloadStatistics
    var canViewRequestLog by Groups.canViewRequestLog
    var maxFiles by Groups.maxFiles
    var maxFileSize by Groups.maxFileSize
}

