package eu.redasurc.tsm.manager.domain.entity

import org.springframework.data.repository.CrudRepository
import java.util.*

interface UserRepository : CrudRepository<User, Long> {
    fun findByIdentitys_Uuid(id: String): User?
    fun findOneByLoginIgnoreCase(login: String): User?
    fun findByEmailIgnoreCase(email: String): User?
}
interface GameRepository : CrudRepository<Game, Long>
interface ClanRepository : CrudRepository<_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.Clan, Long>
interface ClanInviteRepository : CrudRepository<_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanInvite, Long>
interface TokenRepository : CrudRepository<SecurityToken, Long> {
    fun findByToken(token: String) : SecurityToken?
    fun findAllByCreatedDateBefore(expirationDate: Date): List<SecurityToken>
    fun findByTokenAndType(token: String, type: TokenType) : SecurityToken?
}
