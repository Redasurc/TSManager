package eu.redasurc.ts3botV2.entity

import eu.redasurc.ts3botV2.entity.Clan
import eu.redasurc.ts3botV2.entity.Game
import eu.redasurc.ts3botV2.entity.User
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long> {
    fun findByIdentitys_Uuid(id: String): User?
    fun findOneByLogin(login: String): User?
}
interface GameRepository : CrudRepository<Game, Long>
interface ClanRepository : CrudRepository<Clan, Long>
interface ClanInviteRepository : CrudRepository<ClanInvite, Long>
