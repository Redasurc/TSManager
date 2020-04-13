package eu.redasurc.tsm.manager.domain.entity

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface UserRepository : CrudRepository<User, Long> {
    @Query("SELECT t.user FROM TS3Identity t WHERE t.uuid = ?1")
    fun getUserForUUID(uuid: String): User?

    fun findOneByLoginIgnoreCase(login: String): User?
    fun findByEmailIgnoreCase(email: String): User?

    @Query("SELECT a FROM UserAttributes a JOIN FETCH a.user WHERE a.user = ?1 AND a.key = ?2")
    fun getAttributes(user: User, key: String) : UserAttributes?

    @Query("SELECT a FROM UserAttributes a JOIN FETCH a.user WHERE a.key = ?1")
    fun getAttributesForAllUsers(key: String) : List<UserAttributes>
}
interface UserAttributesRepository : CrudRepository<UserAttributes, Long> {
    @Query("SELECT a FROM UserAttributes a WHERE a.user = ?1 AND a.key = ?2")
    fun getAttributes(user: User, key: String) : UserAttributes?
    fun findAllByKey(key: String): List<UserAttributes>
}
interface GameRepository : CrudRepository<Game, Long>
interface ClanRepository : CrudRepository<Clan, Long>
interface ClanInviteRepository : CrudRepository<ClanInvite, Long>
interface TokenRepository : CrudRepository<SecurityToken, Long> {
    fun findByToken(token: String) : SecurityToken?
    fun findAllByCreatedDateBefore(expirationDate: Date): List<SecurityToken>
    fun findByTokenAndType(token: String, type: TokenType) : SecurityToken?
}
