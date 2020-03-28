package eu.redasurc.ts3botV2.security

import eu.redasurc.ts3botV2.entity.ClanPosition
import eu.redasurc.ts3botV2.entity.ServerPermissions.*
import eu.redasurc.ts3botV2.entity.User
import eu.redasurc.ts3botV2.entity.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import java.util.stream.Collectors



/**
 * Spring security user detail service
 */
@Service
class CustomUserDetailsService (private val userRepository: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        return CustomUserDetails(userRepository.findOneByLogin(username)!!)
    }
}



/**
 * Wrapper for User to use in Spring Security
 */
open class CustomUserDetails(user: User) : User(user), UserDetails {

    private val log = LoggerFactory.getLogger(CustomUserDetails::class.java)

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val roles = mutableListOf<String>()

        // Handle global permissions
        roles.add(MEMBER.toString())
        when(permission.level) {
            in MOD.level..Int.MAX_VALUE -> roles.add(MOD.toString())
            in ADMIN.level..Int.MAX_VALUE -> roles.add(ADMIN.toString())
            in SERVERADMIN.level..Int.MAX_VALUE -> roles.add(SERVERADMIN.toString())
        }


        // Handle clan permissions if clan exists
        when(clan?.position?.level) {
            in ClanPosition.MEMBER.level..Int.MAX_VALUE -> roles.add("c_${clan!!.clan.name}_${ClanPosition.MEMBER}")
            in ClanPosition.MOD.level..Int.MAX_VALUE -> roles.add("c_${clan!!.clan.name}_${ClanPosition.MOD}")
            in ClanPosition.ADMIN.level..Int.MAX_VALUE -> roles.add("c_${clan!!.clan.name}_${ClanPosition.ADMIN}")
        }


        return roles
                .stream()
                .map { role ->
                    log.debug("Granting Authority to user with role: " + role.toString())
                    SimpleGrantedAuthority(role.toString())
                }
                .collect(Collectors.toList())
    }

    override fun getPassword(): String {
        return super.pw
    }

    override fun getUsername(): String {
        return super.login
    }

    override fun isEnabled(): Boolean {
        return super.enabled
    }

    override fun isCredentialsNonExpired(): Boolean {
        return super.credentialsNonExpired
    }

    override fun isAccountNonExpired(): Boolean {
        return super.accountNonExpired
    }

    override fun isAccountNonLocked(): Boolean {
        return super.accountNonLocked
    }
}