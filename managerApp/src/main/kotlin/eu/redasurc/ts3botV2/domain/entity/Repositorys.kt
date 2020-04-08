package eu.redasurc.ts3botV2.domain.entity

import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long> {
    fun findByIdentitys_Uuid(id: String): User?
    fun findOneByLoginIgnoreCase(login: String): User?
    fun findByEmailIgnoreCase(email: String): User?
}
interface GameRepository : CrudRepository<Game, Long>
interface ClanRepository : CrudRepository<Clan, Long>
interface ClanInviteRepository : CrudRepository<ClanInvite, Long>
interface TokenRepository : CrudRepository<ActivationToken, Long> {
    fun findByToken(token: String) : ActivationToken?
}
