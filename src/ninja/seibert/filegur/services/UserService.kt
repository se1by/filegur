package ninja.seibert.filegur.services

import io.ktor.auth.Principal
import io.ktor.auth.UserPasswordCredential
import ninja.seibert.filegur.utils.sha512
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

class UserService {
    fun getPrincipal(credential: UserPasswordCredential): UserPrincipal? {
        var user: User? = null
        transaction {
            user = User.find { Users.name eq credential.name }.firstOrNull()
        }
        return if (user?.passwordHash == sha512(user?.salt + credential.password)) {
            UserPrincipal(user!!)
        } else {
            null
        }
    }

    fun getUser(name: String): User? {
        var user: User? = null
        transaction {
            user = User.find { Users.name eq name }.firstOrNull()
        }
        return user
    }

    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        transaction {
            users.addAll(User.all())
        }
        return users
    }

    fun createUser(name: String, salt: String, passwordHash: String, group: Group): User {
        return transaction {
            User.new {
                this.name = name
                this.salt = salt
                this.passwordHash = passwordHash
                this.group = group
            }
        }
    }

    fun updateUser(id: EntityID<Int>, name: String, salt: String, passwordHash: String) {
        transaction {
            val attachedUser = User.findById(id)!!
            attachedUser.name = name
            attachedUser.salt = salt
            attachedUser.passwordHash = passwordHash
        }
    }
}

object Users : IntIdTable() {
    val name = varchar("name", 64).uniqueIndex()
    val salt = varchar("salt", 64)
    val passwordHash = varchar("passwordHash", 512)
    val group = reference("group", Groups)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var name by Users.name
    var salt by Users.salt
    var passwordHash by Users.passwordHash
    val downloadFiles by DownloadFile referrersOn DownloadFiles.owner
    var group by Group referencedOn Users.group
}

data class UserPrincipal(val user: User) : Principal